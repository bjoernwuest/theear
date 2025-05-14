package software.theear.service.auth;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.wicket.Session;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import jakarta.annotation.Nonnull;
import software.theear.service.data.DatabaseService;
import software.theear.service.user.Userprofile;
import software.theear.service.user.UserprofileRepository;
import software.theear.util.FixedExpirationWeakReference;
import software.theear.util.Reference;

/** Logged in user.
 * 
 * @author bjoern@liwuest.net
 */
public final class OidcUser extends DefaultOidcUser {
  private static final long serialVersionUID = -4671186815253070087L;
  private final static Logger log = LoggerFactory.getLogger(OidcUser.class);
  @Autowired private UserprofileRepository m_UserRepository;
  /** Flag to indicate the the user is a root user. This can be revoked only when the user logs out and logs in again. */
  public final boolean isRoot;
  /** The ID of the user profile to this authenticated user. */
  public final UUID UserID;
  /** When this user was first seen by the application. */
  public final Instant CreatedAt;
  /** When this user was last seen by the application. Users not seen for a long time are an indication for inactive users. */
  public final Instant LastSeenAt;
  
  /** Permissions of the user. This member is using lazy-loading from data base as well as timed expiration. The timed expiration means that changes in permission assignment are reflected also to logged in users after a certain amount of time. */
  transient private final FixedExpirationWeakReference<Collection<String>> m_Permissions = new FixedExpirationWeakReference<>(() -> {
    log.trace("Load permissions from data base");
    Collection<String> result = new TreeSet<>();
    try {
      DatabaseService.scheduleTransaction((Conn) -> {
        try (Statement stmt = Conn.createStatement(); PreparedStatement pStmt = Conn.prepareStatement("INSERT INTO __authority_group_ids (group_id) VALUES (?)")) {
          stmt.execute("CREATE TEMPORARY TABLE IF NOT EXISTS __authority_group_ids (group_id UUID) ON COMMIT DROP");
          stmt.execute("TRUNCATE TABLE __authority_group_ids");
          for (GrantedAuthority a : getAuthorities()) {
            pStmt.setObject(1, UUID.fromString(a.getAuthority()));
            pStmt.execute();
          }
          try (ResultSet rSet = stmt.executeQuery("SELECT DISTINCT perm_name FROM auth_functional_permissions WHERE perm_id IN (SELECT DISTINCT perm_id FROM auth_functional_permission_groups_permissions WHERE perm_group_id IN (SELECT DISTINCT perm_group_id FROM auth_functional_permission_group_oidc_group_map WHERE group_id IN (SELECT DISTINCT group_id FROM __authority_group_ids)))")) {
            while (rSet.next()) { result.add(rSet.getString(1)); }
          }
          Conn.rollback();
        }
        return null;
      }).get();
    } catch (ExecutionException Ex) { log.error("Failed to load roles / permissions from data base. See exception for details. In the mean time, this user will have no roles / permissions assigned.", Ex); }
    catch (InterruptedException Ignore) { /* ignore for waiting */ }
    return result;
  });
  
  public OidcUser(@Nonnull Collection<? extends GrantedAuthority> authorities, @Nonnull OidcIdToken idToken, @Nonnull OidcUserInfo userInfo, boolean IsRoot, @Nonnull UUID UserID, @Nonnull Instant CreatedAt, @Nonnull Instant LastSeenAt) {
    super(authorities, idToken, userInfo, "email");
    this.isRoot = IsRoot;
    this.UserID = UserID;
    this.CreatedAt = CreatedAt;
    this.LastSeenAt = LastSeenAt;
  }
  
  /** Returns the user's profile for this authority.
   * 
   * @return The user's profile.
   * @throws NullPointerException May throw this exception if there is no user profile to this user. Usually this shall never happen and can be understood as serious data integrity or business logic error.
   */
  public Reference<Userprofile> getUserprofile() { return this.m_UserRepository.findOne(this.UserID).get(); }
  
  /** Get all roles (authorities) this user has.
   * 
   * @return The roles of this user.
   */
  public Roles getRoles() {
    log.trace("Return roles of user.");
    Roles result = new Roles();
    result.addAll(this.m_Permissions.get());
    return result;
  }
  /** Get all roles (authorities) the user has.
   * 
   * @param User The user to return roles of.
   * @return The roles of the user.
   */
  public static Roles getRoles(OidcUser User) { return (null != User) ? User.getRoles() : new Roles(); }
  
  /** Check if this user has all permissions provided.
   * 
   * @param Permissions The permissions to check for.
   * @return {@code true} if this user has all permissions or is root user.
   */
  public boolean hasAllPermissions(String[] Permissions) {
    if (this.isRoot) return true; 
    return this.m_Permissions.get().containsAll(Arrays.asList(Permissions));
  }
  /** Check if the given user has all permissions provided.
   * 
   * @param User The user to check.
   * @param Permissions The permissions to check for.
   * @return {@code true} if the user has all permissions, or is root user.
   */
  public static boolean hasAllPermissions(OidcUser User, String[] Permissions) { return (null != User) ? User.hasAllPermissions(Permissions) : false; }
  /** Checks if the user linked to given session has the given permissions.
   * 
   * @param Session The session to use.
   * @param Permissions The permissions to check for.
   * @return {@code true} if the user has all permissions, or is root user.
   */
  public static boolean hasAllPermissions(Session Session, String[] Permissions) {
    if (Session instanceof AuthenticatedSession as) { return hasAllPermissions(as.getUser(), Permissions); }
    else return false;
  }
  
  /** Check if this user is root.
   * 
   * The root user usually has all privileges, regardless of any role and permission assignment.
   * 
   * @return {@code true} if this user is a root user.
   */
  public boolean isRoot() { return this.isRoot; }
  
  /** Check if provided user is root.
   * 
   * The root user usually has all privileges, regardless of any role and permission assignment.
   * 
   * @param User The user to check.
   * @return {@code true} if the user is a root user.
   */
  public static boolean isRoot(OidcUser User) { return (null != User) ? User.isRoot() : false; }
}

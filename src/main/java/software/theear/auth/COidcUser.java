package software.theear.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import jakarta.annotation.Nonnull;
import software.theear.data.CDatabaseService;
import software.theear.user.UserProfile;
import software.theear.util.CFixedExpirationWeakReference;

public final class COidcUser extends DefaultOidcUser {
  private static final long serialVersionUID = -4671186815253070087L;
  public final boolean isRoot;
  public final UserProfile user;
  public final Instant createdAt;
  public final Instant lastSeenAt;
  private CDatabaseService m_DBService;
  
  private final CFixedExpirationWeakReference<Collection<String>> m_Permissions = new CFixedExpirationWeakReference<>(() -> {
    // Load permissions from data base
    Collection<String> result = new TreeSet<>();
    try (Connection conn = m_DBService.getConnection(); Statement stmt = conn.createStatement(); PreparedStatement pStmt = conn.prepareStatement("INSERT INTO __authority_group_ids (group_id) VALUES (?)")) {
      stmt.execute("CREATE TEMPORARY TABLE IF NOT EXISTS __authority_group_ids (group_id UUID) ON COMMIT DROP");
      stmt.execute("TRUNCATE TABLE __authority_group_ids");
      for (GrantedAuthority a : getAuthorities()) {
        pStmt.setObject(1, UUID.fromString(a.getAuthority()));
        pStmt.execute();
      }
      try (ResultSet rSet = stmt.executeQuery("SELECT perm_name FROM auth_functional_permissions WHERE perm_id IN (SELECT perm_id FROM auth_functional_permission_groups_permissions WHERE perm_group_id IN (SELECT perm_group_id FROM auth_functional_permission_group_oidc_group_map WHERE group_id IN (SELECT group_id FROM __authority_group_ids)))")) {
        while (rSet.next()) { result.add(rSet.getString(1)); }
      }
      conn.rollback();
    } catch (SQLException Ex) {
      System.err.println(Ex.getMessage()); // FIXME
    }
    return result;
  });
  
  public COidcUser(@Nonnull CDatabaseService DBService, @Nonnull Collection<? extends GrantedAuthority> authorities, @Nonnull OidcIdToken idToken, @Nonnull OidcUserInfo userInfo, boolean IsRoot, @Nonnull UserProfile User, @Nonnull Instant CreatedAt, @Nonnull Instant LastSeenAt) {
    super(authorities, idToken, userInfo, "email");
    this.m_DBService = DBService;
    this.isRoot = IsRoot;
    this.user = User;
    this.createdAt = CreatedAt;
    this.lastSeenAt = LastSeenAt;
  }
  
  public boolean hasAllPermissions(FunctionalPermissionsEnum[] Permissions) {
    if (this.isRoot) return true; 
    return this.m_Permissions.get().containsAll(Arrays.asList(Permissions).stream().map(p -> p.name()).collect(Collectors.toList()));
  }
  public Roles getRoles() {
    Roles result = new Roles();
    result.addAll(this.m_Permissions.get());
    return result;
  }
}

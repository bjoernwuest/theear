package software.theear.auth;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeActions;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiations;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import com.google.common.reflect.ClassPath;

import software.theear.Application;
import software.theear.data.CDatabaseService;
import software.theear.user.NewUserEventbus;
import software.theear.user.UserProfile;

/** Provides user authentication and authorization.
 * 
 * @author bjoern@liwuest.net
 */
@Component public class AuthorizationComponent {
  private final static Logger log = LoggerFactory.getLogger(AuthorizationComponent.class);
	private final CDatabaseService m_DBService;
	private final NewOidcAuthorityEventbus m_NewOidcAuthorityEventbus;
  @Autowired private AuthorizationConfiguration m_AuthConfig;
  @Autowired private NewUserEventbus m_NewUserEventbus;
  
  public AuthorizationComponent(@Autowired CDatabaseService DBService, @Autowired NewOidcAuthorityEventbus NewOidcAuthorityEventbus) {
  	this.m_DBService = DBService;
  	this.m_NewOidcAuthorityEventbus = NewOidcAuthorityEventbus;
  	this.scanAndRegisterFunctionlPermissions();
	}
	
	/** Create and return {@link AuthenticationManager} that simply returns provided {@link Authentication}.
	 * 
	 * The authentication manager would be registered as bean with name {@code authenticationManager} as required by wicket-spring-boot-starter project.
	 * 
	 * @return {@link AuthenticationManager} bean.
	 */
	@Bean(name = "authenticationManager") AuthenticationManager authManager() {
		return new AuthenticationManager() { @Override public Authentication authenticate(Authentication authentication) throws AuthenticationException { return authentication; } };
	}

	/** Configure request chain to only allow authenticated users to access any resource.
	 * 
	 * @param Http Security chain to configure.
	 * @return Configured security chain.
	 * @throws Exception In case building of security chain fails.
	 */
	@Bean SecurityFilterChain filterChain(HttpSecurity Http) throws Exception {
		return Http.authorizeHttpRequests(r -> r.anyRequest().authenticated())
				.oauth2Login(oauth2 -> oauth2.userInfoEndpoint(ep -> ep.oidcUserService(customOidcUserService())))
				.build();
	}

	/** Convert user request (from oauth2 login procedure) into user principal object.
	 * 
	 * The user principal is of type {@link OidcUser}.
	 * 
	 * @return Service to convert request to {@link OidcUser} user principal.
	 */
  private OAuth2UserService<OidcUserRequest, OidcUser> customOidcUserService() {
    final OidcUserService delegate = new OidcUserService();
    
    return userRequest -> {
      boolean isRoot = false;
      OidcUser oidcUser = delegate.loadUser(userRequest);
      Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
      // FIXME: need to make this independent of IdP
      Object idTokenGroups = oidcUser.getIdToken().getClaims().get("groups");
      if ((null != idTokenGroups) && (idTokenGroups instanceof Collection groups)) {
        // Get granted authorities from the user's groups
        for (Object g : groups) {
          try { grantedAuthorities.add(resolveAndRegisterEntraIDGroup(oidcUser.getIssuer().toString(), g.toString())); }
          catch (SQLException Ex) { log.warn("Failed to register groups / roles / permissions for logged in user. The user likely has less permissions than expected. Since this error is not critical, the application continues. See exception for details on the cause.", Ex); }
        }
        // Check if user is in admin_role_id and thus has root privileges
        Object config = m_AuthConfig.registration().get(userRequest.getClientRegistration().getRegistrationId());
        if ((null != config) && (config instanceof Map configMap)) {
          Object adminRole = configMap.get("admin_role_id");
          if ((null != adminRole) && groups.contains(adminRole)) isRoot = true;
        }
      }
      // Persist account
      try (Connection conn = this.m_DBService.getConnection(); PreparedStatement pStmt = conn.prepareStatement("INSERT INTO user_account (oidc_issuer, oidc_subject) VALUES (?, ?) ON CONFLICT (oidc_issuer, oidc_subject) DO UPDATE SET last_seen_at = now() RETURNING user_id, created_at, last_seen_at")) {
        pStmt.setString(1, oidcUser.getIssuer().toString());
        pStmt.setString(2, oidcUser.getSubject());
        try (ResultSet rSet = pStmt.executeQuery()) {
          rSet.next();
          // Create user profile for account
          try (PreparedStatement pStmt2 = conn.prepareStatement("INSERT INTO user_profile (user_id, email, full_name, given_name, family_name) VALUES (?, ?, ?, ?, ?) ON CONFLICT (user_id) DO UPDATE SET email = ?, full_name = ?, given_name = ?, family_name = ?, last_seen_at = now() RETURNING created_at, last_seen_at")) {
            pStmt2.setObject(1, rSet.getObject(1, UUID.class));
            pStmt2.setString(2, oidcUser.getEmail());
            pStmt2.setString(3, oidcUser.getFullName());
            pStmt2.setString(4, oidcUser.getGivenName());
            pStmt2.setString(5, oidcUser.getFamilyName());
            pStmt2.setString(6, oidcUser.getEmail());
            pStmt2.setString(7, oidcUser.getFullName());
            pStmt2.setString(8, oidcUser.getGivenName());
            pStmt2.setString(9, oidcUser.getFamilyName());
            try (ResultSet rSet2 = pStmt2.executeQuery()) {
              conn.commit();
              // Create user profile object
              rSet2.next();
              UserProfile user = new UserProfile(rSet.getObject(1, UUID.class), oidcUser.getEmail(), oidcUser.getFullName(), oidcUser.getGivenName(), oidcUser.getFamilyName(), rSet2.getTimestamp(1).toInstant(), rSet2.getTimestamp(2).toInstant());
              // Announce new user in eventbus
              this.m_NewUserEventbus.send(user);
              // Add user profile to authenticated user object
              COidcUser result = new COidcUser(grantedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), isRoot, user, rSet.getTimestamp(2).toInstant(), rSet.getTimestamp(3).toInstant());
              // Update "best known" user / oidc group mapping
              try (PreparedStatement pStmt3 = conn.prepareStatement("DELETE FROM user_oidc_group_map WHERE user_id = ?"); PreparedStatement pStmt4 = conn.prepareStatement("INSERT INTO user_oidc_group_map (user_id, group_id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
                pStmt3.setObject(1, result.Userprofile.UserID());
                pStmt3.execute();
                pStmt4.setObject(1, result.Userprofile.UserID());
                for (GrantedAuthority a : result.getAuthorities()) {
                  if (a instanceof OidcAuthority oa) {
                    pStmt4.setObject(2, oa.AuthorityID);
                    pStmt4.execute();
                  }
                }
                conn.commit();
              } catch (SQLException Ignore) { log.trace("Failed to write user_id / group_id mapping.", Ignore); /* not too important if this fails */ }
              // Return authenticated user object
              return result;
            }
          }
        }
      } catch (SQLException Ex) { log.error("Failure in application peristence layer. Cannot authenticate user.", Ex); throw new OAuth2AuthenticationException(new OAuth2Error("Failure in application persistence layer. Cannot authenticate user."), Ex); }
    };
  }
  
  /** Resolve group of identity issuer to readable group name and persist in data base.
   * 
   * @param Issuer The issuer of the identity.
   * @param GroupID The group identifier from the issuer.
   * @return Authority object that can be validated against.
   * @throws SQLException If persisting the group fails. Non-persisted groups cannot be used for permission assignment.
   */
  private GrantedAuthority resolveAndRegisterEntraIDGroup(String Issuer, String GroupID) throws SQLException {
    // FIXME: translate group in "Groupname" [which are OID from EntraID] into "reasonable" group names
    
    // IDP groups do not necessarily need to be done in background mode
    try (Connection conn = this.m_DBService.getConnection(); PreparedStatement pStmt = conn.prepareStatement("INSERT INTO auth_oidc_groups (oidc_issuer, oidc_group_name) VALUES (?, ?) ON CONFLICT (oidc_issuer, oidc_group_name) DO UPDATE SET last_seen_at = now() RETURNING group_id, created_at, last_seen_at")) {
      pStmt.setString(1, Issuer);
      pStmt.setString(2, GroupID);
      try (ResultSet rSet = pStmt.executeQuery()) {
        conn.commit();
        rSet.next();
        OidcAuthority authority = new OidcAuthority(Issuer, rSet.getObject(1, UUID.class), GroupID, rSet.getTimestamp(2).toInstant(), rSet.getTimestamp(3).toInstant());
        this.m_NewOidcAuthorityEventbus.send(authority);
        return authority;
      }
    }
  }
  
  /** Create functional permission entries in data base.
   * 
   * @param TypeName The Java type the permission has been detected at.
   * @param OperationName The Java operation (constructor or method) the permission has been detected at.
   * @param PermissionName The actual name of the permission.
   * @param PermissionAction The action linked to the permission. Primarily for {@link org.apache.wicket.authorization.Action}.
   * @param PermissionDescription Descriptional text for the permission.
   */
  private void persistFunctionalPermissionInDatabase(String TypeName, String OperationName, String PermissionName, String PermissionAction, String PermissionDescription) {
    // Suppress depreciation warning on "getConnection" since we are in app init phase.
    try (Connection conn = this.m_DBService.getConnection(); PreparedStatement permStmt = conn.prepareStatement("INSERT INTO auth_functional_permissions (perm_name, perm_action, perm_description) VALUES (?, ?, ?) ON CONFLICT (perm_name, perm_action) DO UPDATE SET last_seen_at = now() RETURNING perm_id")) {
      log.debug("Persist permission '{}' with action '{}' and description '{}' on type '{}' on operation '{}'.", PermissionName, PermissionAction, PermissionDescription, TypeName, OperationName);
      permStmt.setString(1, PermissionName);
      permStmt.setString(2, PermissionAction);
      permStmt.setString(3, PermissionDescription);
      try (ResultSet rSet = permStmt.executeQuery(); PreparedStatement javaStmt = conn.prepareStatement("INSERT INTO auth_functional_permissions_on_java_element (perm_id, type_name, operation_name) VALUES (?, ?, ?) ON CONFLICT (perm_id, type_name, operation_name) DO UPDATE SET last_seen_at = now()")) {
        if (rSet.next()) {
          javaStmt.setObject(1, rSet.getObject(1, UUID.class));
          javaStmt.setString(2, TypeName);
          javaStmt.setString(3, OperationName);
          javaStmt.execute();
        }
      }
      conn.commit();
    } catch (SQLException Ex) { log.warn(String.format("Failed to persist function permission. See exception for details on cause. This error is not critical, so the application continues. Further parameters:\n\tType: {}\n\tOperation: {}\n\tPermission: {}\n\tAction: {}\n\tDescription: {}", TypeName, OperationName, PermissionName, PermissionAction, PermissionDescription), Ex); }
  }
  
  /** Scan, and if found persist, functional permissions on given annotated element.
   * 
   * Following annotations are supported: {@link RequiredFunctionalPermissions}, {@link AuthorizeInstantiation}, {@link AuthorizeAction}, and {@link AuthorizeResource}, and it's grouping variants.
   * 
   * @param TypeName The Java type the annotated element is part of. May be the type itself.
   * @param OperationName The Java operation (constructor or method) the current annotated element is of, or a "place holder" name.
   * @param Element The actual annotated element to scan.
   */
  private void scanAnnotatedElementForFunctionalPermissions(String TypeName, String OperationName, AnnotatedElement Element) {
    log.debug("Scan annotated element '{}' of type '{}' in type '{}' on operation '{}' for permissions.", Element.toString(), Element.getClass().getName(), TypeName, OperationName);
    if (Element.getAnnotation(OneOfRequiredFunctionalPermissions.class) instanceof OneOfRequiredFunctionalPermissions oneOf) {
      for (RequiredFunctionalPermissions reqPerm : oneOf.value()) {
        for (FunctionalPermissionsEnum perm : reqPerm.value()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm.name(), "", perm.description); }
      }
    }
    if (Element.getAnnotation(RequiredFunctionalPermissions.class) instanceof RequiredFunctionalPermissions reqPerm) {
      for (FunctionalPermissionsEnum perm : reqPerm.value()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm.name(), "", perm.description); }
    }
    if (Element.getAnnotation(AuthorizeInstantiations.class) instanceof AuthorizeInstantiations ais) {
      for (AuthorizeInstantiation ai : ais.ruleset()) {
        for (String perm : ai.value()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, "", "Wicket Authorize Instantiation"); }
      }
    }
    if (Element.getAnnotation(AuthorizeInstantiation.class) instanceof AuthorizeInstantiation ai) {
      for (String perm : ai.value()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, "", "Wicket Authorize Instantiation"); }
    }
    
    if (Element.getAnnotation(AuthorizeActions.class) instanceof AuthorizeActions aas) {
      for (AuthorizeAction aa : aas.actions()) {
        for (String perm : aa.deny()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, aa.action(), "Wicket Deny Action"); }
        for (String perm : aa.roles()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, aa.action(), "Wicket Authorize Action"); }
      }
    }
    if (Element.getAnnotation(AuthorizeAction.class) instanceof AuthorizeAction aa) {
      for (String perm : aa.deny()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, aa.action(), "Wicket Deny Action"); }
      for (String perm : aa.roles()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, aa.action(), "Wicket Authorize Action"); }
    }
    
    if (Element.getAnnotation(AuthorizeResource.class) instanceof AuthorizeResource ar) {
      for (String perm : ar.value()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, "", "Wicket Authorize Resource"); }
    }
  }
  
  /** Scan Java classes in this application for functional permissions used.
   * 
   * Those functional permissions can then be assigned to permission groups, which then can be assigned to user roles.
   */
  public void scanAndRegisterFunctionlPermissions() {
    log.debug("Scan all packages, classes and methods for annotation with any permission.");
    try {
      ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses().stream().filter(clsName -> clsName.getPackageName().startsWith(Application.class.getPackageName())).map(clsName -> clsName.load()).forEach(cls -> {
        scanAnnotatedElementForFunctionalPermissions(cls.getName(), "<TYPE>", cls);
        for (Constructor<?> c : cls.getConstructors()) { scanAnnotatedElementForFunctionalPermissions(cls.getName(), c.getName(), c); }
        for (Method m : cls.getMethods()) { scanAnnotatedElementForFunctionalPermissions(cls.getName(), m.toString(), m); }
      });
    } catch (Throwable T) { log.warn("Failed to scan packages, classes and methods for annotations with permissions. See exception for potential causes. This error is not-critical, so the application continues.", T); }
  }
}

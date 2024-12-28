package software.theear.auth;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import com.google.common.reflect.ClassPath;

import software.theear.data.CDatabaseService;

/** Provides user authentication and authorization.
 * 
 * @author bjoern.wuest@gmx.net
 */
@Component public class CAuthorizationComponent {
	@Autowired RAuthorizationConfiguration m_AuthConfig;
	private final CDatabaseService m_DBService;
    private final static Logger log = LoggerFactory.getLogger(CAuthorizationComponent.class);
    
    public CAuthorizationComponent(@Autowired CDatabaseService DBService) {
    	this.m_DBService = DBService;
    	this.scanAndRegisterFunctionalPermissions();
	}
	
	/** Create and return {@link AuthenticationManager} that simply returns provided {@link Authentication}.
	 * 
	 * The authentication manager would be registered as bean with name {@code authenticationManager} as required by wicket-spring-boot-starter project.
	 * 
	 * @return {@link AuthenticationManager} bean.
	 */
	@Bean(name = "authenticationManager") AuthenticationManager authManager() {
		return new AuthenticationManager() {
			@Override public Authentication authenticate(Authentication authentication) throws AuthenticationException { return authentication; }
		};
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
	 * The user principal is of type {@link CNamedOidcUser}.
	 * 
	 * @return Service to convert request to {@link CNamedOidcUser} user principal.
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
        for (Object g : groups) { grantedAuthorities.add(resolveAndRegisterEntraIDGroup(g.toString())); }
        // Check if user is in admin_role_id and thus has root privileges
        Object config = m_AuthConfig.registration().get(userRequest.getClientRegistration().getRegistrationId());
        if ((null != config) && (config instanceof Map configMap)) {
          Object adminRole = configMap.get("admin_role_id");
          if ((null != adminRole) && groups.contains(adminRole)) isRoot = true;
        }
      }
      CNamedOidcUser result = new CNamedOidcUser(grantedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), oidcUser.getName(), isRoot);
      // FIXME: if data base is configured, register user (regardless if the user is present or not)
      return result;
    };
  }
  
  private GrantedAuthority resolveAndRegisterEntraIDGroup(String Groupname) {
    // FIXME: translate groups in "g" [which are OID from EntraID] into "reasonable" group names
    // FIXME: register all groups at data base, so we know whenever there is a new group that then needs to be mapped (m:n-Mapping!)
    return new SimpleGrantedAuthority(Groupname);
  }
  
  private void x(FunctionalPermissions Perm, String MethodName, String ClassName) throws SQLException {
    log.debug("Persist permission '{}' on '{}' from type '{}'.", Perm, MethodName, ClassName);
    try (Connection conn = this.m_DBService.getConnection(); PreparedStatement pStmt = conn.prepareStatement("INSERT INTO auth_functional_permissions (perm_name, perm_description) VALUES (?, ?) ON CONFLICT (perm_name) DO UPDATE SET perm_description = ?, last_seen_at = now() RETURNING perm_id"); PreparedStatement pStmt2 = conn.prepareStatement("INSERT INTO auth_functional_permission_usage (perm_id, used_at_operation, used_at_type) VALUES (?, ?, ?) ON CONFLICT (perm_id, used_at_operation, used_at_type) DO UPDATE SET last_seen_at = now()")) {
      pStmt.setString(1, Perm.name());
      pStmt.setString(2, Perm.description);
      pStmt.setString(3, Perm.description);
      try (ResultSet rSet = pStmt.executeQuery()) {
        if (rSet.next()) {
          pStmt2.setObject(1, rSet.getObject(1, UUID.class));
          pStmt2.setString(2,  MethodName);
          pStmt2.setString(3, ClassName);
          pStmt2.execute();
          conn.commit();
        }
      }
    }
  }
  
  public void scanAndRegisterFunctionalPermissions() {
    log.debug("Scan all classes' methods for implementation of '{}' and '{}' annotation.", RequiredPermissions.class.getName(), OneOfRequiredPermissions.class.getName());
    try {
      ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses().stream().map(clsName -> clsName.load()).forEach(cls -> {
        log.debug("Scan class '{}' for methods annotated by '{}' and '{}'.", cls.getName(), RequiredPermissions.class.getName(), OneOfRequiredPermissions.class.getName());
        for (Method m : cls.getMethods()) {
          if (m.getAnnotation(OneOfRequiredPermissions.class) instanceof OneOfRequiredPermissions oneOf) {
            for (RequiredPermissions reqPerm : oneOf.value()) {
              for (FunctionalPermissions perm : reqPerm.value()) {
                // Persist permission / FIXME: change mode instead of getting connection but instead "post function" that is enqueued and then executed while immediately return the permission of interest for caching
                try { x(perm, m.getName(), cls.getName()); }
                catch (SQLException Ex) {
                  log.error("Failed to write permission '{}' on operation '{}' on type '{}'. See exception for details.", perm.name(), m.getName(), cls.getName(), Ex);
                  System.exit(-1); // FIXME: get error code from constant' class
                }
              }
            }
          }
          if (m.getAnnotation(RequiredPermissions.class) instanceof RequiredPermissions reqPerm) {
            for (FunctionalPermissions perm : reqPerm.value()) {
              // Persist permission / FIXME: change mode instead of getting connection but instead "post function" that is enqueued and then executed while immediately return the permission of interest for caching
              try { x(perm, m.getName(), cls.getName()); }
              catch (SQLException Ex) {
                log.error("Failed to write permission '{}' on operation '{}' on type '{}'. See exception for details.", perm.name(), m.getName(), cls.getName(), Ex);
                System.exit(-1); // FIXME: get error code from constant' class
              }
            }
          }
        }
      });
    } catch (Throwable T) {}
  }
}

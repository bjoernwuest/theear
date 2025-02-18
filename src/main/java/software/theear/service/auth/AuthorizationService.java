package software.theear.service.auth;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.tuple.ImmutablePair;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Service;

import com.google.common.reflect.ClassPath;

import software.theear.Application;
import software.theear.SystemExitReasons;
import software.theear.service.auth.FunctionalPermission.FunctionalPermissionSource;
import software.theear.service.data.DatabaseService;
import software.theear.service.user.Userprofile;
import software.theear.service.user.UserprofileRepository;

/** Provides user authentication and authorization.
 * 
 * @author bjoern@liwuest.net
 */
@Service public final class AuthorizationService {
  /** Class logger */
  private final static Logger log = LoggerFactory.getLogger(AuthorizationService.class);
  /** Singleton-pattern implementation */
  private static AuthorizationService m_THIS = null;
  /** Gets instance of this service. This function may block until the instance is available. */
  public static AuthorizationService getInstance() {
    // Wait for this being initialized
    while (null == m_THIS) {
      log.trace("Wait for Spring framework to construct service.");
      try { Thread.sleep(100); } catch (InterruptedException WakeUp) {}
    }
    return m_THIS;
  }
  
  /** Service configuration */
  @Autowired private AuthorizationConfiguration m_AuthConfig;
  
	/** Internal data structure for all functional permissions. */
  private final Map<UUID, FunctionalPermission> m_FunctionalPermissions = new TreeMap<>();
  /** Internal data structure for all functional permission groups. */
  private final Map<UUID, FunctionalPermissionGroup> m_FunctionalPermissionGroups = new TreeMap<>();
  
  
  /** Eventually persist functional permission if it does not exist in data base.
   * 
   * @param PermissionName The actual name of the permission.
   * @param PermissionAction The action linked to the permission. Primarily for {@link org.apache.wicket.authorization.Action}.
   * @param PermissionDescription Descriptional text for the permission.
   * @param TypeName The Java type the permission has been detected at.
   * @param OperationName The Java operation (constructor or method) the permission has been detected at.
   * @throws ExecutionException If anything goes wrong while executing this function. Eventually see nested exception for details.
   */
  private void m_MaybeCreate(String PermissionName, String PermissionDescription, String TypeName, String OperationName) throws ExecutionException {
    try {
      DatabaseService.scheduleTransaction((Conn) -> {
        try (PreparedStatement permStmt = Conn.prepareStatement("INSERT INTO auth_functional_permissions (perm_name, perm_description) VALUES (?, ?) ON CONFLICT (perm_name) DO UPDATE SET last_seen_at = now() RETURNING perm_id, created_at, last_seen_at")) {
          log.debug("Persist permission '{}' and description '{}' on type '{}' on operation '{}'.", PermissionName, PermissionDescription, TypeName, OperationName);
          permStmt.setString(1, PermissionName);
          permStmt.setString(2, PermissionDescription);
          try (ResultSet rSet = permStmt.executeQuery(); PreparedStatement javaStmt = Conn.prepareStatement("INSERT INTO auth_functional_permissions_on_java_element (perm_id, type_name, operation_name) VALUES (?, ?, ?) ON CONFLICT (perm_id, type_name, operation_name) DO UPDATE SET last_seen_at = now() RETURNING created_at, last_seen_at")) {
            rSet.next();
            UUID permID = rSet.getObject(1, UUID.class);
            javaStmt.setObject(1, permID);
            javaStmt.setString(2, TypeName);
            javaStmt.setString(3, OperationName);
            try (ResultSet rSet2 = javaStmt.executeQuery()) {
              rSet2.next();
              boolean signal = false;
              if (!m_FunctionalPermissions.containsKey(permID)) { m_FunctionalPermissions.put(permID, new FunctionalPermission(permID, PermissionName, PermissionDescription, rSet.getTimestamp(2).toInstant(), rSet.getTimestamp(3).toInstant())); signal = true; }
              m_FunctionalPermissions.get(permID)._FunctionalPermissionSources.add(new FunctionalPermission.FunctionalPermissionSource(TypeName, OperationName, rSet2.getTimestamp(1).toInstant(), rSet2.getTimestamp(2).toInstant()));
              FunctionalPermission result = m_FunctionalPermissions.get(permID);
              if (signal) { EBNewFunctionalPermission.GET.send(result); }
              return result;
            }
          }
        }
      }).get();
    } catch (InterruptedException Ignore) { /* do not react on interruption */ }
  }
  
  /** Create functional permission entries in data base.
   * 
   * @param PermissionName The actual name of the permission.
   * @param PermissionDescription Descriptional text for the permission.
   * @param TypeName The Java type the permission has been detected at.
   * @param OperationName The Java operation (constructor or method) the permission has been detected at.
   */
  private void persistFunctionalPermissionInDatabase(String TypeName, String OperationName, String PermissionName, String PermissionDescription) {
    try { this.m_MaybeCreate(PermissionName, PermissionDescription, TypeName, OperationName); }
    catch (ExecutionException Ex) {log.warn(String.format("Failed to persist function permission. See exception for details on cause. This error is not critical, so the application continues. Further parameters:\n\tType: {}\n\tOperation: {}\n\tPermission: {}\n\tAction: {}\n\tDescription: {}", TypeName, OperationName, PermissionName, PermissionDescription), Ex); }
  }
  
  /** Scan, and if found persist, functional permissions on given annotated element.
   * 
   * Following annotations are supported: {@link RequiredFunctionalPermissions}, {@link AuthorizeInstantiation}, {@link AuthorizeAction}, and {@link AuthorizeResource}, and it's grouping variants.
   * 
   * @param TypeName The Java type the annotated element is part of. May be the type itself.
   * @param OperationName The Java operation (constructor or method) the current annotated element is of, or a "place holder" name.
   * @param Element The actual annotated element to scan.
   */
  private void m_ScanAnnotatedElementForFunctionalPermissions(String TypeName, String OperationName, AnnotatedElement Element) {
    log.debug("Scan annotated element '{}' of type '{}' in type '{}' on operation '{}' for permissions.", Element.toString(), Element.getClass().getName(), TypeName, OperationName);
    if (Element.getAnnotation(OneOfRequiredFunctionalPermissions.class) instanceof OneOfRequiredFunctionalPermissions oneOf) {
      for (RequiredFunctionalPermissions reqPerm : oneOf.value()) {
        for (String perm : reqPerm.value()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, "Required functional permission"); }
      }
    }
    if (Element.getAnnotation(RequiredFunctionalPermissions.class) instanceof RequiredFunctionalPermissions reqPerm) {
      for (String perm : reqPerm.value()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, "Required functional permission"); }
    }
    if (Element.getAnnotation(AuthorizeInstantiations.class) instanceof AuthorizeInstantiations ais) {
      for (AuthorizeInstantiation ai : ais.ruleset()) {
        for (String perm : ai.value()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, "Wicket Authorize Instantiation"); }
      }
    }
    if (Element.getAnnotation(AuthorizeInstantiation.class) instanceof AuthorizeInstantiation ai) {
      for (String perm : ai.value()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, "Wicket Authorize Instantiation"); }
    }
    
    if (Element.getAnnotation(AuthorizeActions.class) instanceof AuthorizeActions aas) {
      for (AuthorizeAction aa : aas.actions()) {
        for (String perm : aa.deny()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, "Wicket Deny Action"); }
        for (String perm : aa.roles()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, "Wicket Authorize Action"); }
      }
    }
    if (Element.getAnnotation(AuthorizeAction.class) instanceof AuthorizeAction aa) {
      for (String perm : aa.deny()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, "Wicket Deny Action"); }
      for (String perm : aa.roles()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, "Wicket Authorize Action"); }
    }
    
    if (Element.getAnnotation(AuthorizeResource.class) instanceof AuthorizeResource ar) {
      for (String perm : ar.value()) { persistFunctionalPermissionInDatabase(TypeName, OperationName, perm, "Wicket Authorize Resource"); }
    }
  }
  
  /** Scan Java classes in this application for functional permissions used.
   * 
   * Those functional permissions can then be assigned to permission groups, which then can be assigned to user roles.
   */
  private void m_ScanAndRegisterFunctionlPermissions() {
    log.debug("Scan all packages, classes and methods for annotation with any permission.");
    try {
      ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses().stream().filter(clsName -> clsName.getPackageName().startsWith(Application.class.getPackageName())).map(clsName -> clsName.load()).forEach(cls -> {
        m_ScanAnnotatedElementForFunctionalPermissions(cls.getName(), "<TYPE>", cls);
        for (Constructor<?> c : cls.getConstructors()) { m_ScanAnnotatedElementForFunctionalPermissions(cls.getName(), c.getName(), c); }
        for (Method m : cls.getMethods()) { m_ScanAnnotatedElementForFunctionalPermissions(cls.getName(), m.toString(), m); }
      });
    } catch (Throwable T) { log.warn("Failed to scan packages, classes and methods for annotations with permissions. See exception for potential causes. This error is not-critical, so the application continues.", T); }
  }
  
  /** Reads the functional permissions from data base and stores them in memory.
   * 
   * @throws ExecutionException if there is any problem in processing the function. Most likely nested exception knows more.
   * @throws InterruptedException indicates that for whatever reason this function was aborted. Most likely due to shutdown request.
   */
  private void m_ReadFunctionalPermissionsFromDatabase() throws ExecutionException, InterruptedException {
    log.debug("Read all functional permissions from data base into memory.");
    DatabaseService.scheduleTransaction((Conn) -> {
      try (Statement stmt = Conn.createStatement(); ResultSet rSet = stmt.executeQuery("SELECT perm_id, perm_name, perm_description, created_at, last_seen_at FROM auth_functional_permissions")) {
        while (rSet.next()) {
          UUID fpid = rSet.getObject(1, UUID.class);
          m_FunctionalPermissions.putIfAbsent(fpid, new FunctionalPermission(fpid, rSet.getString(2), rSet.getString(3), rSet.getTimestamp(4).toInstant(), rSet.getTimestamp(5).toInstant()));
        }
      }
      try (Statement stmt = Conn.createStatement(); ResultSet rSet = stmt.executeQuery("SELECT perm_id, type_name, operation_name, created_at, last_seen_at FROM auth_functional_permissions_on_java_element")) {
        while (rSet.next()) {
          UUID fpid = rSet.getObject(1, UUID.class);
          FunctionalPermission fperm = m_FunctionalPermissions.get(fpid);
          if (null != fperm) { fperm._FunctionalPermissionSources.add(new FunctionalPermissionSource(rSet.getString(2), rSet.getString(3), rSet.getTimestamp(4).toInstant(), rSet.getTimestamp(5).toInstant())); }
        }
      }
      return null;
    }).get();
  }
  
  /** Reads the functional permission groups from data base and stores them in memory.
   * 
   * @throws ExecutionException if there is any problem in processing the function. Most likely nested exception knows more.
   * @throws InterruptedException indicates that for whatever reason this function was aborted. Most likely due to shutdown request.
   */
  private void m_ReadFunctionalPermissionGroupsFromDatabase() throws ExecutionException, InterruptedException {
    DatabaseService.scheduleTransaction((Conn) -> {
      try (Statement stmt = Conn.createStatement(); ResultSet rSet = stmt.executeQuery("SELECT perm_group_id, perm_group_name, perm_group_description, created_at FROM auth_functional_permission_groups")) {
        while (rSet.next()) {
          m_FunctionalPermissionGroups.put(rSet.getObject(1, UUID.class), new FunctionalPermissionGroup(this, rSet.getObject(1, UUID.class), rSet.getString(2), rSet.getString(3), rSet.getTimestamp(4).toInstant()));
        }
      }
      return null;
    }).get();
  }
  
  /** Load the assignments of functional permissions to functional permission groups.
   * 
   * @throws ExecutionException if there is any problem in processing the function. Most likely nested exception knows more.
   * @throws InterruptedException indicates that for whatever reason this function was aborted. Most likely due to shutdown request.
   */
  private void m_ReadFunctionalPermissionToGroupsAssignmentFromDatabase() throws ExecutionException, InterruptedException {
    DatabaseService.scheduleTransaction((Conn) -> {
      try (Statement stmt = Conn.createStatement(); ResultSet rSet = stmt.executeQuery("SELECT perm_group_id, perm_id FROM auth_functional_permission_groups_permissions")) {
        while (rSet.next()) {
          FunctionalPermissionGroup fpg = m_FunctionalPermissionGroups.get(rSet.getObject(1, UUID.class));
          FunctionalPermission fp = m_FunctionalPermissions.get(rSet.getObject(2, UUID.class));
          fpg._AssignedFunctionalPermissions.add(fp);
          fp._AssignedFunctionalPermissionGroups.add(fpg);
        }
      }
      return null;
    }).get();
  }
  
  /** Initialize service, e.g. waiting for dependency injection to finish. */
  private void m_InitializeService() {
    // Get all fields of this class that depend on Dependency Injection
    log.debug("Ensure dependency injection is finished.");
    Collection<Field> dependencies = new LinkedList<>();
    for (Field f : this.getClass().getDeclaredFields()) {
      if (null != f.getAnnotation(Autowired.class)) { dependencies.add(f); }
    }
    while (dependencies.stream().anyMatch(e -> { try { if (null == e.get(this)) { return true; } } catch (Exception Ignore) {} return false; })) {
      log.trace("Waiting for dependency injection to finish.");
      try { Thread.sleep(100); } catch (InterruptedException Ignore) { /* waited */ }
    }
    
    // Scan system for static information
    this.m_ScanAndRegisterFunctionlPermissions();
    
    // Pre-load data into memory
    try {
      try {
        this.m_ReadFunctionalPermissionsFromDatabase();
        this.m_ReadFunctionalPermissionGroupsFromDatabase();
        this.m_ReadFunctionalPermissionToGroupsAssignmentFromDatabase();
        
      } catch (ExecutionException Ex) {
        log.error("Failure during service initialization. See exception for details. Will terminate application.", Ex);
        System.exit(SystemExitReasons.FailedToInitialize.ExitCode);
      }
      // FIXME: signal full initialization of this service!
    } catch (InterruptedException Ignore) { /* Ignore, most likely shutdown was requested */ }
  }

  
  AuthorizationService() throws IllegalStateException {
    synchronized (log) {
      if (null != m_THIS) { throw new IllegalStateException("This service has been instantiated already. Use " + AuthorizationService.class.getName() + ".getInstance() instead."); }
      new Thread(new Runnable() { public void run() { m_InitializeService(); } }, this.getClass().getName() + " - Functional Permission Scan").start();
      m_THIS = this;
    }
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
	  // TODO: the ".csrf(csrf -> csrf.disable())" is insecure; find a solution!
		return Http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(r -> r.anyRequest().authenticated())
				.oauth2Login(oauth2 -> oauth2.userInfoEndpoint(ep -> ep.oidcUserService(customOidcUserService())))
				.build();
	}

	/** Convert user request (from oauth2 login procedure) into user principal object.
	 * 
	 * The user principal is of type {@link OidcUser}.
	 * 
	 * @return Service to convert request to {@link OidcUser} user principal.
	 */
  private OAuth2UserService<OidcUserRequest, org.springframework.security.oauth2.core.oidc.user.OidcUser> customOidcUserService() {
    final OidcUserService delegate = new OidcUserService();
    
    return userRequest -> {
      boolean isRoot = false;
      org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser = delegate.loadUser(userRequest);
      Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
      // FIXME: need to make this independent of IdP
      Object idTokenGroups = oidcUser.getIdToken().getClaims().get("groups");
      if ((null != idTokenGroups) && (idTokenGroups instanceof Collection groups)) {
        // Get granted authorities from the user's groups
        for (Object g : groups) {
          try {
            log.trace("User {} has group {}.", oidcUser.getEmail(), g.toString());
            grantedAuthorities.add(resolveAndRegisterEntraIDGroup(oidcUser.getIssuer().toString(), g.toString()));
          } catch (Exception Ex) { log.warn("Failed to register groups / roles / permissions for logged in user. The user likely has less permissions than expected. Since this error is not critical, the application continues. See exception for details on the cause.", Ex); }
        }
        // Check if user is in admin_role_id and thus has root privileges
        Object config = m_AuthConfig.registration().get(userRequest.getClientRegistration().getRegistrationId());
        if ((null != config) && (config instanceof Map configMap)) {
          Object adminRole = configMap.get("admin_role_id");
          if ((null != adminRole) && groups.contains(adminRole)) isRoot = true;
        }
      }
      // Persist account
      final boolean isR = isRoot;
      try {
        return DatabaseService.scheduleTransaction((Conn) -> {
          try (PreparedStatement pStmt =  Conn.prepareStatement("INSERT INTO user_account (oidc_issuer, oidc_subject) VALUES (?, ?) ON CONFLICT (oidc_issuer, oidc_subject) DO UPDATE SET last_seen_at = now() RETURNING user_id, created_at, last_seen_at")) {
            pStmt.setString(1, oidcUser.getIssuer().toString());
            pStmt.setString(2, oidcUser.getSubject());
            try (ResultSet rSet = pStmt.executeQuery()) {
              Conn.commit();
              rSet.next();
              // Create and get user profile for account
              Userprofile user = UserprofileRepository.getInstance().createAndReturn(rSet.getObject(1, UUID.class), oidcUser.getEmail(), oidcUser.getFullName(), oidcUser.getGivenName(), oidcUser.getFamilyName()).get().get();
              // Add user profile to authenticated user object
              OidcUser result = new OidcUser(grantedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), isR, user.UserID, rSet.getTimestamp(2).toInstant(), rSet.getTimestamp(3).toInstant());
              // Update "best known" user / oidc group mapping
              try (PreparedStatement pStmt2 = Conn.prepareStatement("DELETE FROM user_oidc_group_map WHERE user_id = ?"); PreparedStatement pStmt3 =  Conn.prepareStatement("INSERT INTO user_oidc_group_map (user_id, group_id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
                pStmt2.setObject(1, user.UserID);
                pStmt2.execute();
                pStmt3.setObject(1, user.UserID);
                for (GrantedAuthority a : result.getAuthorities()) {
                  if (a instanceof OidcAuthority oa) {
                    pStmt3.setObject(2, oa.AuthorityID);
                    pStmt3.execute();
                  }
                }
              } catch (SQLException Ignore) { log.trace("Failed to write user_id / group_id mapping.", Ignore); /* not too important if this fails */ }
              // Return authenticated user object
              return result;
            }
          }
        }).get();
      } catch (InterruptedException | ExecutionException Ex) {
        log.error("Failure in application peristence layer. Cannot authenticate user.", Ex); throw new OAuth2AuthenticationException(new OAuth2Error("Failure in application persistence layer. Cannot authenticate user."), Ex);
      }
    };
  }
  
  /** Resolve group of identity issuer to readable group name and persist in data base.
   * 
   * @param Issuer The issuer of the identity.
   * @param GroupID The group identifier from the issuer.
   * @return Authority object that can be validated against.
   * @throws SQLException If persisting the group fails. Non-persisted groups cannot be used for permission assignment.
   */
  private GrantedAuthority resolveAndRegisterEntraIDGroup(String Issuer, String GroupID) throws Exception {
    // FIXME: translate group in "Groupname" [which are OID from EntraID] into "reasonable" group names
    
    // IDP groups do not necessarily need to be done in background mode
    return DatabaseService.scheduleTransaction((Conn) -> {
      try (PreparedStatement pStmt = Conn.prepareStatement("INSERT INTO auth_oidc_groups (oidc_issuer, oidc_group_name) VALUES (?, ?) ON CONFLICT (oidc_issuer, oidc_group_name) DO UPDATE SET last_seen_at = now() RETURNING group_id, created_at, last_seen_at")) {
        pStmt.setString(1, Issuer);
        pStmt.setString(2, GroupID);
        try (ResultSet rSet = pStmt.executeQuery()) {
          rSet.next();
          OidcAuthority authority = new OidcAuthority(Issuer, rSet.getObject(1, UUID.class), GroupID, rSet.getTimestamp(2).toInstant(), rSet.getTimestamp(3).toInstant());
          EBNewOidcAuthority.GET.send(authority);
          return authority;
        }
      }
    }).get();
  }
  
  

  /** Updates functional permission group information in data base.
   * 
   * @param Group The functional permission group whose information to update.
   * @return {@code true} if the update was successful.
   */
  public void update(@Nonnull FunctionalPermissionGroup Group) {
    log.debug("Update functional permission group {}", Group);
    try {
      String[] v = DatabaseService.scheduleTransaction((Conn) -> {
        try (PreparedStatement pStmt = Conn.prepareStatement("UPDATE auth_functional_permission_groups SET perm_group_name = ?, perm_group_description = ? WHERE perm_group_id = ? RETURNING perm_group_name, perm_group_description")) {
          pStmt.setString(1, Group.Name());
          pStmt.setString(2, Group.Description());
          pStmt.setObject(3, Group.FunctionalPermissionGroupID);
          try (ResultSet rSet = pStmt.executeQuery()) {
            rSet.next();
            EBFunctionalPermissionGroupUpdate.GET.send(Group.FunctionalPermissionGroupID);
            return new String[] {rSet.getString(1), rSet.getString(2)};
          }
        }
      }).get();
      Group._Name(v[0]);
      Group._Description(v[1]);
      EBFunctionalPermissionGroupUpdate.GET.send(Group.FunctionalPermissionGroupID);
    } catch (ExecutionException Ex) { log.warn("Could not update information on functional permission group. See exception for details. Application continues.", Ex); }
    catch (InterruptedException Ignore) { /* simple wait */ }
  }
  
  /** Returns functional permission, if found. Otherwise, the returned {@link Optional} may contain {@code null}.
   * 
   * @param FunctionalPermissionID The ID of the functional permission to get.
   * @return Optional of the found functional permission.
   */
  public Optional<FunctionalPermission> getFunctionalPermission(UUID FunctionalPermissionID) {  return Optional.ofNullable(this.m_FunctionalPermissions.get(FunctionalPermissionID)); }
  
  /** Returns an unmodifiable view on references to all known functional permissions.
   * 
   * The returned collection is not thread safe.
   * 
   * @return Unmodifiable view on references to all known functional permissions.
   */
  public Collection<FunctionalPermission> getAllFunctionalPermissions() { return Collections.unmodifiableCollection(this.m_FunctionalPermissions.values()); }
  
  /** Create a new functional permission group.
   * 
   * @param Name The name of the functional permission group to create.
   * @param Description The description text of the functional permission.
   * @return Reference to the functional permission, or {@code null} if there was any problem.
   */
  public FunctionalPermissionGroup createFunctionalPermissionGroup(String Name, String Description) {
    synchronized (this.m_FunctionalPermissionGroups) {
      return this.m_FunctionalPermissionGroups.values().stream().filter(fpg -> fpg.Name().equalsIgnoreCase(Name.trim())).findAny().orElseGet(() -> {
        try {
          FunctionalPermissionGroup result = DatabaseService.scheduleTransaction(Conn -> {
            try (PreparedStatement pStmt = Conn.prepareStatement("INSERT INTO auth_functional_permission_groups (perm_group_name, perm_group_description) VALUES (?, ?) ON CONFLICT (perm_group_name) DO NOTHING RETURNING perm_group_id, created_at")) {
              pStmt.setString(1,  Name);
              pStmt.setString(2,  Description);
              try (ResultSet rSet = pStmt.executeQuery()) {
                rSet.next();
                FunctionalPermissionGroup fpg = new FunctionalPermissionGroup(getInstance(), rSet.getObject(1, UUID.class), Name, Description, rSet.getTimestamp(2).toInstant());
                this.m_FunctionalPermissionGroups.put(fpg.FunctionalPermissionGroupID, fpg);
                return fpg;
              }
            }
          }).get();
          EBNewFunctionalPermissionGroup.GET.send(result);
          return result;
        } catch (Exception Ex) {
          log.warn("Failed to create functional permission group in data base. See exeption for details.", Ex);
          return null;
        }
      });
    }
  }
  
  public void deleteFunctionalPermissionGroup(UUID FunctionalPermissionGroupID) {
    FunctionalPermissionGroup fpg = this.m_FunctionalPermissionGroups.get(FunctionalPermissionGroupID);
    if (null != fpg) {
      // FIXME: implement!
      System.out.println("DELETE FUNCTIONAL PERMISSION");
      EBDeletedFunctionalPermissionGroup.GET.send(this.m_FunctionalPermissionGroups.get(FunctionalPermissionGroupID));
    }
  }
  
  /** Returns functional permission group, if found. Otherwise, the returned {@link Optional} may contain {@code null}.
   * 
   * @param FunctionalPermissionGroupID The ID of the functional permission group to get.
   * @return Optional of the found functional permission group.
   */
  public Optional<FunctionalPermissionGroup> getFunctionalPermissionGroup(UUID FunctionalPermissionGroupID) { return Optional.ofNullable(this.m_FunctionalPermissionGroups.get(FunctionalPermissionGroupID)); }
  
  /** Returns an unmodifiable view on references to all known functional permission groups.
   * 
   * The returned collection is not thread safe.
   * 
   * @return Unmodifiable view on references to all known functional permission groups.
   */
  public Collection<FunctionalPermissionGroup> getAllFunctionalPermissionGroups() { return Collections.unmodifiableCollection(this.m_FunctionalPermissionGroups.values()); }
  
  /** Grant functional permission to functional permission group.
   * 
   * @param FunctionalPermission The functional permission to actually grant.
   * @param FunctionalPermissionGroup The functional permission group whom to grant for.
   */
  public void grant(@Nonnull FunctionalPermission FunctionalPermission, @Nonnull FunctionalPermissionGroup FunctionalPermissionGroup) {
    if (FunctionalPermissionGroup._AssignedFunctionalPermissions.add(FunctionalPermission) && FunctionalPermission._AssignedFunctionalPermissionGroups.add(FunctionalPermissionGroup)) {
      DatabaseService.scheduleTransaction((Conn) -> {
        try (PreparedStatement pStmt = Conn.prepareStatement("INSERT INTO auth_functional_permission_groups_permissions (perm_group_id, perm_id) VALUES (?, ?) ON CONFLICT (perm_group_id, perm_id) DO NOTHING")) {
          pStmt.setObject(1, FunctionalPermissionGroup.FunctionalPermissionGroupID);
          pStmt.setObject(2, FunctionalPermission.FunctionalPermissionID);
          pStmt.execute();
        }
        return null;
      });
      EBFunctionlPermissionGranted.GET.send(new ImmutablePair<>(FunctionalPermission, FunctionalPermissionGroup));
    }
  }
  
  /** Revoke functional permission from permission group.
   * 
   * @param FunctionalPermission The functional permission to revoke.
   * @param FunctionalPermissionGroup The functional permission group to revoke from.
   */
  public void revoke(@Nonnull FunctionalPermission FunctionalPermission, @Nonnull FunctionalPermissionGroup FunctionalPermissionGroup) {
    if (FunctionalPermissionGroup._AssignedFunctionalPermissions.remove(FunctionalPermission) || FunctionalPermission._AssignedFunctionalPermissionGroups.remove(FunctionalPermissionGroup)) {
      DatabaseService.scheduleTransaction((Conn) -> {
        try (PreparedStatement pStmt = Conn.prepareStatement("DELETE FROM auth_functional_permission_groups_permissions WHERE perm_group_id = ? AND perm_id = ?")) {
          pStmt.setObject(1, FunctionalPermissionGroup.FunctionalPermissionGroupID);
          pStmt.setObject(2, FunctionalPermission.FunctionalPermissionID);
          pStmt.execute();
        }
        return null;
      });
      EBFunctionalPermissionRevoked.GET.send(new ImmutablePair<>(FunctionalPermission, FunctionalPermissionGroup));
    }
  }
}

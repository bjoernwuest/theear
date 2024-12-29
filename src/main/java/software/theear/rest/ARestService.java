package software.theear.rest;

import java.util.Optional;

import org.apache.wicket.request.http.WebRequest;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.contenthandling.json.objserialdeserial.GsonObjectSerialDeserial;
import org.wicketstuff.rest.contenthandling.json.webserialdeserial.JsonWebSerialDeserial;
import org.wicketstuff.rest.resource.AbstractRestResource;
import org.wicketstuff.rest.resource.MethodMappingInfo;
import org.wicketstuff.restutils.wicket.AttributesWrapper;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import software.theear.auth.CNamedOidcUser;
import software.theear.auth.OneOfRequiredPermissions;
import software.theear.auth.RequiredPermissions;

/** Base class for Wicketstuff'ed rest services.
 * 
 * Use this class instead of directly deriving from AbstractRestResource for simplified registration as well as support for permission checking using {@link RequiredPermissions} and {@link OneOfRequiredPermissions}.
 * 
 * @author bjoern.wuest@gmx.net
 */
@RestService public abstract class ARestService extends AbstractRestResource<JsonWebSerialDeserial> {
  private static final long serialVersionUID = 791899004161345758L;
  private final String m_RootPath;
  
  /** Subclasses must super(...) this constructor to actually ensure proper service registration.
   * 
   * @param RootPath The root path of this rest resource.
   * 
   * FIXME: add validation so RootPath is a valid REST URL path!
   */
  protected ARestService(@Nonnull String RootPath) {
    super(new JsonWebSerialDeserial(new GsonObjectSerialDeserial()));
    this.m_RootPath = RootPath.startsWith("/") ? RootPath : "/" + RootPath;
  }
  
  /** Get the URI root path this service shall register at.
   * 
   * @return The URI root path this service shall register at.
   */
  final String getRootPath() { return this.m_RootPath; }
  
  /** Returns the user that this session is linked to.
   * 
   * To be successful, requires that there is a session linked to the current invocation.
   * 
   * @return Optional with the user that this session is linked to. If there is no user linked to the session, then the optional is empty.
   */
  protected final Optional<CNamedOidcUser> p_GetSessionUser() {
	WebRequest wr = super.getCurrentWebRequest();
  	if (null != wr) {
        Object cr = wr.getContainerRequest();
        if ((cr instanceof HttpServletRequest hsr) 
            && (hsr.getUserPrincipal() instanceof OAuth2AuthenticationToken at) 
            && (at.getPrincipal() instanceof CNamedOidcUser namedUser))
            { return Optional.of(namedUser); }
  	}
    return Optional.empty();
  }

  @Override protected final void onBeforeMethodInvoked(MethodMappingInfo mappedMethod, Attributes attributes) {
    super.onBeforeMethodInvoked(mappedMethod, attributes);
    
    // Prepare check for sufficient permissions
    boolean sufficientPermissions = false;
    // Only check if this is an annotated REST method
    if (null != mappedMethod.getMethod().getAnnotation(MethodMapping.class)) {
      // Get the user of the session doing the request
      Optional<CNamedOidcUser> sessionUser = p_GetSessionUser();
      // We have at least a REST method, try to get annotations for permissions; check "groups of permissions" first
      OneOfRequiredPermissions oneOfPerms = mappedMethod.getMethod().getAnnotation(OneOfRequiredPermissions.class);
      RequiredPermissions perms = mappedMethod.getMethod().getAnnotation(RequiredPermissions.class);
      if (null != oneOfPerms) {
    	if (sessionUser.isPresent()) { // Only check for permissions if there is user
          for (RequiredPermissions p : oneOfPerms.value()) {
            sufficientPermissions = sessionUser.get().hasAllPermissions(p.value());
            if (sufficientPermissions) break; // If we have sufficient permissions, do not check further
          }
        }
      } else if ((null != perms) && sessionUser.isPresent()) { sufficientPermissions = sessionUser.get().hasAllPermissions(perms.value()); }
      else sufficientPermissions = true; // There are no permissions configured, so anyone can access
    } else sufficientPermissions = true; // This is no REST method so anyone can access
    
    // If session user does not has sufficient permissions, show unauthorized use
    if (!sufficientPermissions) unauthorizedMethodAccess((new AttributesWrapper(attributes)).getWebResponse(), mappedMethod);
    
    // Now, invoke the other "onBefore"
    onBefore(mappedMethod, attributes);
  }
  
  @Override protected final void onAfterMethodInvoked(MethodMappingInfo mappedMethod, Attributes attributes, Object result) {
    onAfter(mappedMethod, attributes, result);
    super.onAfterMethodInvoked(mappedMethod, attributes, result);
  }
  
  /** Invoked before actual method invocation.
   * 
   * Stub for finalized {@link #onBeforeMethodInvoked(MethodMappingInfo, Attributes)}.
   * 
   * @param mappedMethod The method to be invoked. See {@link #onBeforeMethodInvoked(MethodMappingInfo, Attributes)} for details.
   * @param attributes Attributes for the invocation. See {@link #onBeforeMethodInvoked(MethodMappingInfo, Attributes)} for details.
   */
  protected void onBefore(MethodMappingInfo mappedMethod, Attributes attributes) {}
  /** Invoked after method invocation.
   * 
   * Stub for finalized {@link #onAfterMethodInvoked(MethodMappingInfo, Attributes, Object)}.
   * 
   * @param mappedMethod The method that has been invoked. See {@link #onAfterMethodInvoked(MethodMappingInfo, Attributes, Object)} for details.
   * @param attributes Attributes after the invocation. See {@link #onAfterMethodInvoked(MethodMappingInfo, Attributes, Object)} for details.
   * @param result Result of the invocation. See {@link #onAfterMethodInvoked(MethodMappingInfo, Attributes, Object)} for details.
   */
  protected void onAfter(MethodMappingInfo mappedMethod, Attributes attributes, Object result) {}
}

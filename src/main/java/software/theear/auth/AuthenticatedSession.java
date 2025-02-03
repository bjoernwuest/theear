package software.theear.auth;

import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.request.Request;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import com.giffing.wicket.spring.boot.starter.configuration.extensions.external.spring.security.SecureWebSession;

import jakarta.servlet.http.HttpServletRequest;

/** Implementation of secure web session that carries information on the authenticated used, if present.
 * 
 * @author bjoern@liwuest.net 
 */
public class AuthenticatedSession extends SecureWebSession {
  private static final long serialVersionUID = 1139594419058439059L;
  private OidcUser m_User;

  public AuthenticatedSession(Request request) {
    super(request);
    Object cr = request.getContainerRequest();
    if ((cr instanceof HttpServletRequest hsr)
        && (hsr.getUserPrincipal() instanceof OAuth2AuthenticationToken at)
        && (at.getPrincipal() instanceof OidcUser namedUser))
        { this.m_User = namedUser; }
  }
  
  /** Returns the user logged in to this session.
   * 
   * @return The user logged in. May be {@code null} if no user is logged in.
   */
  public OidcUser getUser() { return this.m_User; }
  /** Check if the user has "root" privileges, i.e. is permitted any action regardless of any permission assignment.
   * 
   * @return {@code true} if the user of this session has "root" privileges. {@code false} if not, or there is no user logged in to this session.
   */
  public boolean isUserRoot() { return OidcUser.isRoot(this.m_User); }
  
  @Override public Roles getRoles() { return OidcUser.getRoles(this.m_User); }
}

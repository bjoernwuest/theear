package software.theear.auth;

import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.request.Request;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import com.giffing.wicket.spring.boot.starter.configuration.extensions.external.spring.security.SecureWebSession;

import jakarta.servlet.http.HttpServletRequest;

public class CSession extends SecureWebSession {
  private static final long serialVersionUID = 1139594419058439059L;
  private CNamedOidcUser m_User;

  public CSession(Request request) {
    super(request);
    Object cr = request.getContainerRequest();
    if ((cr instanceof HttpServletRequest hsr)
        && (hsr.getUserPrincipal() instanceof OAuth2AuthenticationToken at)
        && (at.getPrincipal() instanceof CNamedOidcUser namedUser))
        { this.m_User = namedUser; }
  }
  
  public CNamedOidcUser getUser() { return this.m_User; }
  
  @Override public Roles getRoles() { return this.m_User.getRoles(); }
}

package software.theear;

import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.springframework.stereotype.Component;

import com.giffing.wicket.spring.boot.starter.app.WicketBootSecuredWebApplication;

import software.theear.auth.CSession;
import software.theear.auth.CWicketAuthorizationStrategy;

@Component public class CWicketApplication extends WicketBootSecuredWebApplication {

  @Override protected void init() {
    super.init();
    this.getSecuritySettings().setAuthorizationStrategy(new CWicketAuthorizationStrategy());
  }

  @Override protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() { return CSession.class; }
  
}
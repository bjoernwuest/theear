package software.theear;

import org.apache.wicket.Page;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.springframework.stereotype.Component;

import com.giffing.wicket.spring.boot.starter.app.WicketBootSecuredWebApplication;

import software.theear.auth.AuthenticatedSession;
import software.theear.auth.WicketAuthorizationStrategy;

@Component public class WicketApplication extends WicketBootSecuredWebApplication {
  @Override protected void init() {
    super.init();
    this.getSecuritySettings().setAuthorizationStrategy(new WicketAuthorizationStrategy());
  }

  @Override protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() { return AuthenticatedSession.class; }
  
  // FIXME: depending on status of application, return one or the other home page
  @Override public Class<? extends Page> getHomePage() { return HomePage.class; }

}
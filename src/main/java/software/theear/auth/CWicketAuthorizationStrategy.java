package software.theear.auth;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.request.component.IRequestableComponent;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;

import software.theear.rest.ARestService;

public class CWicketAuthorizationStrategy implements IAuthorizationStrategy {
  @Override
  public <T extends IRequestableComponent> boolean isInstantiationAuthorized(Class<T> componentClass) {
    // TODO Auto-generated method stub
    Session s = Session.get();
    if (s instanceof CSession secSess) {
      System.out.println(secSess.getRoles());
    }
    return false;
  }

  @Override
  public boolean isActionAuthorized(Component component, Action action) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isResourceAuthorized(IResource resource, PageParameters parameters) {
    // Permit any REST service since authorization check is done via REST procedures
    if (ARestService.class.isInstance(resource)) return true;
    // TODO Auto-generated method stub
    return false;
  }
}

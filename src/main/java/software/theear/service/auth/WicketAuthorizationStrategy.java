package software.theear.service.auth;

import java.util.Arrays;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeActions;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiations;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeResource;
import org.apache.wicket.request.component.IRequestableComponent;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Authorization strategy for Wicket supporting {@link OidcUser} in {@link AuthenticatedSession}.
 * 
 * @author bjoern@liwuest.net
 */
public class WicketAuthorizationStrategy implements IAuthorizationStrategy {
  private final static Logger log = LoggerFactory.getLogger(WicketAuthorizationStrategy.class);
  /** Check class to instantiate by Wicket annotations if user of session has all the required roles (permissions).
   * 
   * @param ClassToInstantiate The class to instantiate by Wicket
   * @param UserRoles The roles the user has
   * @return {@code true} if user has sufficient roles, {@code false} otherwise
   */
  private boolean checkInsantiation(Class<?> ClassToInstantiate, Roles UserRoles) {
    log.trace("Check permission to instantiate class '{}'", ClassToInstantiate.getName());
    boolean result = false;
    if (ClassToInstantiate.getAnnotation(AuthorizeInstantiations.class) instanceof AuthorizeInstantiations groupAnnon) {
      for (AuthorizeInstantiation annon : groupAnnon.ruleset()) { result |= UserRoles.containsAll(Arrays.asList(annon.value())); }
    } else if (ClassToInstantiate.getAnnotation(AuthorizeInstantiation.class) instanceof AuthorizeInstantiation annon) { result |= UserRoles.containsAll(Arrays.asList(annon.value())); }
    else result = true; // The class to instantiate has no annotation, so grant access by default!
    return result;
  }
  @Override public <T extends IRequestableComponent> boolean isInstantiationAuthorized(Class<T> componentClass) {
    // If there is a session, do the permission check
    if (Session.get() instanceof AuthenticatedSession secSess) { return secSess.isUserRoot() || checkInsantiation(componentClass, secSess.getRoles()); }
    // Otherwise, fail if class is annotated, or succeed if not
    return checkInsantiation(componentClass, new Roles());
  }

  /** Check action on (Wicket) component if user of session has all the required roles (permissions).
   * 
   * @param Comp The actual component to check action for.
   * @param Act The action to check.
   * @param UserRoles The roles the user has.
   * @return {@code true} if user has sufficient roles, {@code false} otherwise
   */
  private boolean checkAction(Component Comp, Action Act, Roles UserRoles) {
    log.trace("Check permission to run action {} in component '{}'", Act.getName(), Comp.getPath());
    boolean result = false;
    if (Comp.getClass().getAnnotation(AuthorizeActions.class) instanceof AuthorizeActions groupAnnon) {
      for (AuthorizeAction annon : groupAnnon.actions()) {
        if (Act.getName() == annon.action()) {
          for (String deny : annon.deny()) { if (UserRoles.contains(deny)) return false; } // Early denial if any denied role is with user
          result |= UserRoles.containsAll(Arrays.asList(annon.roles()));
        }
      }
    } else if (Comp.getClass().getAnnotation(AuthorizeAction.class) instanceof AuthorizeAction annon) {
      if (Act.getName() == annon.action()) {
        for (String deny : annon.deny()) { if (UserRoles.contains(deny)) return false; } // Early denial if any denied role is with user
        result |= UserRoles.containsAll(Arrays.asList(annon.roles()));
      }
    } else result = true; // The class to instantiate has no annotation, so grant access by default!
    return result;
  }
  @Override public boolean isActionAuthorized(Component component, Action action) {
    // If there is a session, do the permission check
    if (Session.get() instanceof AuthenticatedSession secSess) { return secSess.isUserRoot() || checkAction(component, action, secSess.getRoles()); }
    // Otherwise, fail if class is annotated, or succeed if not
    return checkAction(component, action, new Roles());
  }

  /** Check if user of session has all the required roles (permissions) to use the given resource.
   * 
   * @param Resource The actual resource.
   * @param Parameters The parameters of the page the resource is applied to.
   * @param UserRoles The roles of the user.
   * @return {@code true} if user has sufficient roles, {@code false} otherwise
   */
  private boolean checkResource(IResource Resource, PageParameters Parameters, Roles UserRoles) {
    log.trace("Check permission for resource {} with page parameters {}.", Resource.toString(), Parameters.toString());
    boolean result = false;
    if (Resource.getClass().getAnnotation(AuthorizeResource.class) instanceof AuthorizeResource annon) { result |= UserRoles.containsAll(Arrays.asList(annon.value())); }
    else result = true;
    return result;
  }
  @Override public boolean isResourceAuthorized(IResource resource, PageParameters parameters) {
    // If there is a session, do the permission check
    if (Session.get() instanceof AuthenticatedSession secSess) { return secSess.isUserRoot() || checkResource(resource, parameters, secSess.getRoles()); }
    // Otherwise, fail if class is annotated, or succeed if not
    return checkResource(resource, parameters, new Roles());
  }
}

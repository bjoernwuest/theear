package software.theear.auth;

import org.apache.commons.lang3.tuple.ImmutablePair;

import software.theear.event.SimpleEventbus;
import software.theear.util.Reference;

/** Signal revocation of functional permission from functional permission group.
 *
 * @author bjoern@liwuest.net
 */
public class EBFunctionalPermissionRevoked extends SimpleEventbus<ImmutablePair<Reference<FunctionalPermission>, Reference<FunctionalPermissionGroup>>> {
  private EBFunctionalPermissionRevoked() {}
  public final static EBFunctionalPermissionRevoked GET = new EBFunctionalPermissionRevoked();
}
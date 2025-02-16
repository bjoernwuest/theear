package software.theear.service.auth;

import org.apache.commons.lang3.tuple.ImmutablePair;

import software.theear.util.SimpleEventbus;

/** Signal revocation of functional permission from functional permission group.
 *
 * @author bjoern@liwuest.net
 */
public class EBFunctionalPermissionRevoked extends SimpleEventbus<ImmutablePair<FunctionalPermission, FunctionalPermissionGroup>> {
  private EBFunctionalPermissionRevoked() {}
  public final static EBFunctionalPermissionRevoked GET = new EBFunctionalPermissionRevoked();
}
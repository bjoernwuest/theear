package software.theear.service.auth;

import org.apache.commons.lang3.tuple.ImmutablePair;

import software.theear.util.SimpleEventbus;

/** Signal that functional permission was granted to functional permission group.
 * 
 * @author bjoern@liwuest.net
 */
public class EBFunctionalPermissionGranted extends SimpleEventbus<ImmutablePair<FunctionalPermission, FunctionalPermissionGroup>> {
  private EBFunctionalPermissionGranted() {}
  public final static EBFunctionalPermissionGranted GET = new EBFunctionalPermissionGranted();
}

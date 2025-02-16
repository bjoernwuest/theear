package software.theear.service.auth;

import org.apache.commons.lang3.tuple.ImmutablePair;

import software.theear.util.SimpleEventbus;

/** Signal that functional permission was granted to functional permission group.
 * 
 * @author bjoern@liwuest.net
 */
public class EBFunctionlPermissionGranted extends SimpleEventbus<ImmutablePair<FunctionalPermission, FunctionalPermissionGroup>> {
  private EBFunctionlPermissionGranted() {}
  public final static EBFunctionlPermissionGranted GET = new EBFunctionlPermissionGranted();
}

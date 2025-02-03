package software.theear.auth;

import org.apache.commons.lang3.tuple.ImmutablePair;

import software.theear.event.SimpleEventbus;
import software.theear.util.Reference;

/** Signal that functional permission was granted to functional permission group.
 * 
 * @author bjoern@liwuest.net
 */
public class EBFunctionlPermissionGranted extends SimpleEventbus<ImmutablePair<Reference<FunctionalPermission>, Reference<FunctionalPermissionGroup>>> {
  private EBFunctionlPermissionGranted() {}
  public final static EBFunctionlPermissionGranted GET = new EBFunctionlPermissionGranted();
}

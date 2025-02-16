package software.theear.service.auth;

import software.theear.util.SimpleEventbus;

/** Signal detection of new functional permission.
 * 
 * @author bjoern@liwuest.net
 */
public final class EBNewFunctionalPermission extends SimpleEventbus<FunctionalPermission> {
  private EBNewFunctionalPermission() {}
  public final static EBNewFunctionalPermission GET = new EBNewFunctionalPermission();
}

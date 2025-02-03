package software.theear.auth;

import software.theear.event.SimpleEventbus;
import software.theear.util.Reference;

/** Signal detection of new functional permission.
 * 
 * @author bjoern@liwuest.net
 */
public final class EBNewFunctionalPermission extends SimpleEventbus<Reference<FunctionalPermission>> {
  private EBNewFunctionalPermission() {}
  public final static EBNewFunctionalPermission GET = new EBNewFunctionalPermission();
}

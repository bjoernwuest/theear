package software.theear.service.auth;

import java.util.UUID;

import software.theear.util.SimpleEventbus;

/** Signals updates on functional permission group updates.
 * 
 * @author bjoern@liwuest.net
 */
public class EBFunctionalPermissionGroupUpdate extends SimpleEventbus<UUID> {
  private EBFunctionalPermissionGroupUpdate() {}
  public final static EBFunctionalPermissionGroupUpdate GET = new EBFunctionalPermissionGroupUpdate();
}

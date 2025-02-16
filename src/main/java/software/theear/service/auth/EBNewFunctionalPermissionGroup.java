package software.theear.service.auth;

import software.theear.util.SimpleEventbus;

public class EBNewFunctionalPermissionGroup extends SimpleEventbus<FunctionalPermissionGroup> {
  private EBNewFunctionalPermissionGroup() {}
  public final static EBNewFunctionalPermissionGroup GET = new EBNewFunctionalPermissionGroup();
}

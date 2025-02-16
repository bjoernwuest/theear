package software.theear.service.auth;

import software.theear.util.SimpleEventbus;

public class EBDeletedFunctionalPermissionGroup extends SimpleEventbus<FunctionalPermissionGroup> {
  private EBDeletedFunctionalPermissionGroup() {}
  public final static EBDeletedFunctionalPermissionGroup GET = new EBDeletedFunctionalPermissionGroup();
}

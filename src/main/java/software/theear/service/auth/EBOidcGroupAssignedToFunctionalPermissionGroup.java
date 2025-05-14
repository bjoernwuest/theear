package software.theear.service.auth;

import org.apache.commons.lang3.tuple.Pair;

import software.theear.util.SimpleEventbus;

public class EBOidcGroupAssignedToFunctionalPermissionGroup extends SimpleEventbus<Pair<OidcGroup, FunctionalPermissionGroup>>{
  private EBOidcGroupAssignedToFunctionalPermissionGroup() {}
  public final static EBOidcGroupAssignedToFunctionalPermissionGroup GET = new EBOidcGroupAssignedToFunctionalPermissionGroup();
}

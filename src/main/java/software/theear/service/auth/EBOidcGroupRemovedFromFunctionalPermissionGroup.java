package software.theear.service.auth;

import org.apache.commons.lang3.tuple.Pair;

import software.theear.util.SimpleEventbus;

public class EBOidcGroupRemovedFromFunctionalPermissionGroup extends SimpleEventbus<Pair<OidcGroup, FunctionalPermissionGroup>>{
  private EBOidcGroupRemovedFromFunctionalPermissionGroup() {}
  public final static EBOidcGroupRemovedFromFunctionalPermissionGroup GET = new EBOidcGroupRemovedFromFunctionalPermissionGroup();
}

package software.theear.demo;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.parameters.RequestParam;

import jakarta.annotation.Nonnull;
import software.theear.auth.CNamedOidcUser;
import software.theear.auth.FunctionalPermissionsEnum;
import software.theear.auth.OneOfRequiredFunctionalPermissions;
import software.theear.auth.RequiredFunctionalPermissions;
import software.theear.rest.ARestService;
import software.theear.rest.RestService;


@RestService("/api") public class CDemoRestService extends ARestService {
  private static final long serialVersionUID = 2094674776918625824L;
  private final Logger log = LoggerFactory.getLogger(CDemoRestService.class);
  
  @Nonnull
  @OneOfRequiredFunctionalPermissions({@RequiredFunctionalPermissions({FunctionalPermissionsEnum.Hello, FunctionalPermissionsEnum.World}), @RequiredFunctionalPermissions({FunctionalPermissionsEnum.World, FunctionalPermissionsEnum.Out})})
  @RequiredFunctionalPermissions({FunctionalPermissionsEnum.Hello, FunctionalPermissionsEnum.Out})
  @MethodMapping("/test")
  public String test(@RequestParam(value = "continue", required = false) Object Ignore) {
  	Optional<CNamedOidcUser> user = p_GetSessionUser();
  	log.info("test function - GET /: user={}", user);
  	if (user.isPresent()) { return user.get().userName(); }
  	return "test";
  }
}

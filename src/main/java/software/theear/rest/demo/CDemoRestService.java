package software.theear.rest.demo;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.parameters.RequestParam;

import jakarta.annotation.Nonnull;
import software.theear.rest.AbstractRestService;
import software.theear.rest.RootPath;
import software.theear.service.auth.OidcUser;
import software.theear.service.auth.OneOfRequiredFunctionalPermissions;
import software.theear.service.auth.RequiredFunctionalPermissions;


@RootPath("/api/demo") public class CDemoRestService extends AbstractRestService {
  private static final long serialVersionUID = 2094674776918625824L;
  private final Logger log = LoggerFactory.getLogger(CDemoRestService.class);
  
  @Nonnull
  @OneOfRequiredFunctionalPermissions({@RequiredFunctionalPermissions({"Hello", "World"}), @RequiredFunctionalPermissions({"World", "Out"})})
  @RequiredFunctionalPermissions({"Hello", "Out"})
  @MethodMapping("/test")
  public String test(@RequestParam(value = "continue", required = false) Object Ignore) {
  	Optional<OidcUser> user = p_GetSessionUser();
  	log.info("test function - GET /: user={}", user);
  	if (user.isPresent()) { return user.get().getEmail(); }
  	return "test";
  }
}

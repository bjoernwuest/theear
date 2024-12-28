package software.theear.demo;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.parameters.RequestParam;

import jakarta.annotation.Nonnull;
import software.theear.auth.CNamedOidcUser;
import software.theear.auth.FunctionalPermissions;
import software.theear.auth.OneOfRequiredPermissions;
import software.theear.auth.RequiredPermissions;
import software.theear.rest.ARestService;
import software.theear.rest.RestService;


@RestService public class CDemoRestService extends ARestService {
  private static final long serialVersionUID = 2094674776918625824L;
  private final Logger log = LoggerFactory.getLogger(CDemoRestService.class);
  
  public CDemoRestService() { super("/api"); }
  
  @Nonnull
  @OneOfRequiredPermissions({@RequiredPermissions({FunctionalPermissions.Hello, FunctionalPermissions.World}), @RequiredPermissions({FunctionalPermissions.World, FunctionalPermissions.Out})})
  @RequiredPermissions({FunctionalPermissions.Hello, FunctionalPermissions.Out})
  @MethodMapping("/test") public String test(@RequestParam(value = "continue", required = false) Object Ignore) {
	  // FIXME: do not use @AuthorizeAnnotation but instead do the checking here; eventually write own annotation and security checker, integrated into onBeforeMethodInvoke...?
//	log.info("test function - authorities={}", User.getAuthorities());
	Optional<CNamedOidcUser> user = p_GetSessionUser();
	log.info("test function - GET /: user={}", user);
	if (user.isPresent()) { return user.get().userName(); }
	return "test";
  }
}

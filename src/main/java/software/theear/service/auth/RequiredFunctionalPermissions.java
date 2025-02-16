package software.theear.service.auth;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Lists permissions required. All given permissions must be satisfied.
 * 
 * @author bjoern@liwuest.net
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface RequiredFunctionalPermissions {
  String[] value();
}

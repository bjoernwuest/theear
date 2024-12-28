package software.theear.auth;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Lists permissions required. All given permissions must be satisfied.
 * 
 * @author bjoern.wuest@gmx.net
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface RequiredPermissions {
  FunctionalPermissions[] value();
}

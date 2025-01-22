package software.theear.auth;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Group multiple permission sets.
 * 
 * One of the given permission set needs to be satisfied. That is, {@link RequiredFunctionalPermissions} means that all listed permissions must be satisfied. With this annotation, multiple {@link RequiredFunctionalPermissions} can be grouped where only one of them needs to be satisfied.
 * 
 * @author bjoern@liwuest.net
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface OneOfRequiredFunctionalPermissions {
  RequiredFunctionalPermissions[] value();
}

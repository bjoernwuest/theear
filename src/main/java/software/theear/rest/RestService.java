package software.theear.rest;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/** Annotation to indicate that class is a REST service.
 * 
 * Usually, only {@link AbstractRestService} needs to be marked by this. REST services shall derive from {@link AbstractRestService}.
 * 
 * @author bjoern@liwuest.net
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Component
@Inherited
public @interface RestService {
  @AliasFor(annotation = Component.class)
  String value() default "";
}

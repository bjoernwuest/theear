package software.theear.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/** Marker annotation to identify classes implementing REST endpoints.
 * 
 * Classes being market with this annotation must also derive from {@link ARestService}. 
 * 
 * @author bjoern.wuest@gmx.net
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Component
public @interface RestService {
  @AliasFor(annotation = Component.class)
  String value() default "";
}

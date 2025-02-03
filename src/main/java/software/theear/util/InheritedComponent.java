package software.theear.util;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/** Marker for Spring Component that can be inherited. Meant to be applied to base classes and interfaces, e.g. for persistence.
 * 
 * @author bjoern@liwuest.net
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Component
@Inherited
public @interface InheritedComponent {}

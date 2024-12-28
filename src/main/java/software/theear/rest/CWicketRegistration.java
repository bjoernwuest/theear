package software.theear.rest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import com.giffing.wicket.spring.boot.context.extensions.WicketApplicationInitConfiguration;

import software.theear.util.CHardReference;

/** Searches the application for classes extending from {@link ARestService}.
 * 
 * Classes found will be instantiated 
 * 
 * @author bjoern.wuest@gmx.net
 */
@Component public class CWicketRegistration implements WicketApplicationInitConfiguration {
  private final static Logger log = LoggerFactory.getLogger(CWicketRegistration.class);
  
  @Override public void init(WebApplication webApplication) {
    log.debug("Register '{}' annotation to be scanned for.", RestService.class.getName());
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(RestService.class));
    
    String packageName = CWicketRegistration.class.getPackageName();
    packageName = packageName.substring(0, packageName.lastIndexOf('.'));
    log.debug("Scan all packages below and including '{}'", packageName);
    
    for (BeanDefinition bd : provider.findCandidateComponents(packageName)) {
      log.debug("Going to load class for rest service '{}'", bd.getBeanClassName());
      try {
        final Class<?> cls = Class.forName(bd.getBeanClassName());
        log.debug("Check if rest service '{}' is subclassing from '{}'", cls.getName(), ARestService.class.getName());
        if (ARestService.class.isAssignableFrom(cls)) {
          log.debug("Get constructor for rest service, either accepting '{}' or default constructor without any argument", WebApplication.class.getName());
          final CHardReference<ARestService> serviceReference = new CHardReference<>();
          try {
            Constructor<?> cons = cls.getDeclaredConstructor(WebApplication.class);
            log.debug("Found constructor on rest service '{}' accepting '{}'. Going to instantiate.", cls.getName(), WebApplication.class.getName());
            serviceReference.set(ARestService.class.cast(cons.newInstance(webApplication)));
          } catch (NoSuchMethodException TryDefaultConstructor) {
            try {
              Constructor<?> cons = cls.getDeclaredConstructor();
              log.debug("Found default constructor on rest service '{}'. Going to instantiate.", cls.getName());
              serviceReference.set(ARestService.class.cast(cons.newInstance()));
            } catch (NoSuchMethodException Ignore) { log.debug("No valid constructor found on rest service '{}'. Skipping.", cls.getName()); }
          }
          if (null != serviceReference.get()) {
            log.debug("Could instantiate rest service '{}'. Mount on '{}'", cls.getName(), serviceReference.get().getRootPath());
            webApplication.mountResource(serviceReference.get().getRootPath(), new ResourceReference("RestService-" + cls.getName()) {
              private static final long serialVersionUID = -1;
              @Override public IResource getResource() { return serviceReference.get(); }
            });
          }
        }
      } catch (ClassNotFoundException Ex) { log.debug("Failed to load class for rest service '{}'", bd.getBeanClassName()); }
      catch (InvocationTargetException | IllegalAccessException | InstantiationException Ex) { log.debug("Failed to instantiate rest service '{}'. Exception '{}' with message '{}'.", bd.getBeanClassName(), Ex.getClass().getName(), Ex.getMessage()); }
    }
  }
}

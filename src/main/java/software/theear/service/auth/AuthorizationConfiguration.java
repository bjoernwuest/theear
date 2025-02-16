package software.theear.service.auth;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration of spring.security.oauth2.client subsection in application.yaml
 * 
 * @author bjoern@liwuest.net
 */
@ConfigurationProperties(prefix = "spring.security.oauth2.client") public record AuthorizationConfiguration(Map<String, Object> registration) {}

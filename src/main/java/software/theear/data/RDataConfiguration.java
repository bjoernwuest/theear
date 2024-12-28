package software.theear.data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.web.util.UriComponentsBuilder;

@ConfigurationProperties("database")
public record RDataConfiguration(@DefaultValue("localhost") String host, @DefaultValue("5432") int port, @DefaultValue("theear") String database, @DefaultValue("theear") String username, @DefaultValue("theear") String password, @DefaultValue("") String options) {
	public RDataConfiguration {
		if (1 > port) { port = 5432; }
		options = UriComponentsBuilder.fromUriString(options).build().getQuery();
		if ((null != options) && !options.startsWith("?")) options = "?" + options;
	}
}

package software.theear;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

import software.theear.rest.RestService;
import software.theear.util.InheritedComponent;

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScans(@ComponentScan(basePackages = "software.theear", includeFilters = {@ComponentScan.Filter(InheritedComponent.class), @ComponentScan.Filter(RestService.class)}))
public class Application {
  public final static void main(String[] Args) {
    new SpringApplicationBuilder().sources(Application.class).run(Args);
  }
}

// FIXME: later readings
// https://www.coderdreams.com/wicket-autocomplete-components-with-remote-data/
// https://www.coderdreams.com/adding-fuzzy-search-to-wicket-autocomplete-components/
// https://www.coderdreams.com/how-to-use-wicket-component-events/
// https://www.coderdreams.com/creating-single-click-buttons-with-wicket/
// https://www.coderdreams.com/understanding-wicket-models/
// https://www.coderdreams.com/basics-of-wicket-ajax-requests/
// 
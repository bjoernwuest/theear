package software.theear;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.giffing.wicket.spring.boot.context.scan.WicketHomePage;

@WicketHomePage
public class HomePage extends WebPage {
  private static final long serialVersionUID = 8919240523233153885L;

  public HomePage(final PageParameters Parameters) {
    super(Parameters);
    add(new Label("root", getApplication().getFrameworkSettings().getVersion()));
  }
}

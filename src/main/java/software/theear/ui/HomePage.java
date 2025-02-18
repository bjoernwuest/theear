package software.theear.ui;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import software.theear.ui.auth.FunctionalPermissionGroupOverviewPanel;

public class HomePage extends WebPage {
  private static final long serialVersionUID = 8919240523233153885L;

  public HomePage(final PageParameters Parameters) {
    super(Parameters);
    add(new Header("header"));
//    add(new Label("main", this.getApplication().getFrameworkSettings().getVersion()));
    add(new FunctionalPermissionGroupOverviewPanel("main"));
    add(new Footer("footer"));
  }
}

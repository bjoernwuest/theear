package software.theear.ui;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public class Header extends Panel {

  private static final long serialVersionUID = -2791308799161277018L;

  public Header(String id) {
    super(id);
    add(new Label("logo", "Logo"));
    add(new Label("breadcrumb", "Breadcrumbs"));
    add(new Label("quickactions", "Quick actions"));
    // TODO Auto-generated constructor stub
  }

}

package software.theear.ui;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public final class Footer extends Panel {

  private static final long serialVersionUID = -1017963385075119384L;

  public Footer(String id) {
    super(id);
    
    add(new Label("systemstatus", "System status"));
    add(new Label("systemnotifications", "System notifications"));
    // TODO Auto-generated constructor stub
  }

}

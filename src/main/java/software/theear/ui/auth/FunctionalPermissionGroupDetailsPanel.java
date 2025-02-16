package software.theear.ui.auth;

import java.util.LinkedList;

import javax.annotation.Nonnull;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

import software.theear.service.auth.FunctionalPermission;
import software.theear.service.auth.FunctionalPermissionGroup;

public class FunctionalPermissionGroupDetailsPanel extends Panel {
  private static final long serialVersionUID = -6403088453097327149L;

  public FunctionalPermissionGroupDetailsPanel(@Nonnull String id, @Nonnull FunctionalPermissionGroup FPGroup) {
    super(id);
    
    add(new ListView<FunctionalPermission>("functionalPermissions", new LinkedList<FunctionalPermission>(FPGroup.AssignedFunctionalPermissions)) {
      private static final long serialVersionUID = 7595827256754906532L;

      @Override protected void populateItem(ListItem<FunctionalPermission> item) {
        item.add(new Label("functionalPermissionName", item.getModel().map(e -> e.PermissionName)));
        item.add(new Label("description", item.getModel().map(e -> e.PermissionDescription)));
        item.add(new Label("knownSince", item.getModel().map(e -> e.CreatedAt.toString())));
        item.add(new Label("lastDetected", item.getModel().map(e -> e.LastSeenAt.toString())));
      }
    });
  }
}

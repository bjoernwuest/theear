package software.theear.ui.auth;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.wicket.Component;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiations;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import software.theear.service.auth.AuthorizationService;
import software.theear.service.auth.EBDeletedFunctionalPermissionGroup;
import software.theear.service.auth.EBNewFunctionalPermissionGroup;
import software.theear.service.auth.FunctionalPermissionGroup;
import software.theear.service.auth.OidcUser;

@AuthorizeInstantiations(ruleset = {
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read}), 
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.add}),
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.delete}),
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.edit})
})
public class FunctionalPermissionGroupOverviewPanel extends Panel {
  private final static String read = "readFunctionalPermissionGroup";
  private final static String add = "addFunctionalPermissionGroup";
  private final static String delete = "deleteFunctionalPermissionGroup";
  private final static String edit = "editFunctionalPermissionGroup";
  private static final long serialVersionUID = -6842093369143884412L;
  
  
  private final static class FunctionalPermissionGroupModel implements IModel<List<FunctionalPermissionGroup>> {
    private static final long serialVersionUID = -992790658583847524L;

    private final static class FunctionalPermissionGroupList extends AbstractList<FunctionalPermissionGroup> implements Serializable {
      private static final long serialVersionUID = 3874351659133626347L;
      private final LinkedList<UUID> m_FunctionalPermissionGroupIDs = new LinkedList<>();
      
      private FunctionalPermissionGroupList() {
        // Lock to prevent concurrent modifications
        synchronized (this.m_FunctionalPermissionGroupIDs) {
          // FIXME: implement "set" behavior for list!
          EBNewFunctionalPermissionGroup.GET.add(fpg -> { if (null != fpg) { synchronized (this.m_FunctionalPermissionGroupIDs) { this.m_FunctionalPermissionGroupIDs.addLast(fpg.FunctionalPermissionGroupID); } } });
          EBDeletedFunctionalPermissionGroup.GET.add(fpg -> { if (null != fpg) { synchronized (this.m_FunctionalPermissionGroupIDs) { this.m_FunctionalPermissionGroupIDs.remove(fpg.FunctionalPermissionGroupID); } } });
          this.m_FunctionalPermissionGroupIDs.addAll(AuthorizationService.getInstance().getAllFunctionalPermissionGroups().stream().map(e -> e.FunctionalPermissionGroupID).collect(Collectors.toList()));
        }
      }
      
      @Override public int size() { synchronized (this.m_FunctionalPermissionGroupIDs) { return this.m_FunctionalPermissionGroupIDs.size(); } }
      @Override public FunctionalPermissionGroup get(int index) { synchronized (this.m_FunctionalPermissionGroupIDs) { return AuthorizationService.getInstance().getFunctionalPermissionGroup(this.m_FunctionalPermissionGroupIDs.get(index)).orElseGet(null); } }
    };
    
    private final List<FunctionalPermissionGroup> m_List = new FunctionalPermissionGroupList();
    @Override public List<FunctionalPermissionGroup> getObject() { return this.m_List; }
  }

  
  public FunctionalPermissionGroupOverviewPanel(@Nonnull String id) {
    super(id);
    
    // Build "new permission group form"
    Form<Void> newFunctionalPermissionGroupForm = new Form<>("newFunctionalPermissionGroupForm");
    TextField<String> tf = new TextField<>("newNameInput", Model.of("")); // TODO: add model that checks for duplicate names and shows warning!
    newFunctionalPermissionGroupForm.add(tf);
    newFunctionalPermissionGroupForm.add(new Button("createNewGroup") {
      private static final long serialVersionUID = 2275603002457204186L;

      @Override public void onSubmit() {
        if (null != AuthorizationService.getInstance().createFunctionalPermissionGroup(tf.getValue(), "")) {
          tf.setModel(Model.of("")); // TODO: add model that checks for duplicate names and shows warning! [see line above!]
          // TODO: display "success message"
        };
    }});
    add(newFunctionalPermissionGroupForm);
    // Disable and hide for if user has insufficient permissions
    if (!OidcUser.hasAllPermissions(this.getSession(), new String[] {add})) { newFunctionalPermissionGroupForm.setEnabled(false).setVisible(false); }
    
    boolean canEdit = OidcUser.hasAllPermissions(this.getSession(), new String[]{edit});
    boolean canDelete = OidcUser.hasAllPermissions(this.getSession(), new String[]{delete});
    
    PageableListView<FunctionalPermissionGroup> plv = new PageableListView<>("functionalPermissionGroupsList", new FunctionalPermissionGroupModel(), 3) {
      private static final long serialVersionUID = 1786621734119480353L;
      
      @Override protected void onConfigure() {
        super.onConfigure();
        this.setVisible(!this.getList().isEmpty());
      }

      @Override protected void populateItem(ListItem<FunctionalPermissionGroup> item) {
        item.add(new Form<>("functionalPermissionGroupsListForm").add(new TextField<String>("name", new IModel<String>() {
          private static final long serialVersionUID = 7733493013501344701L;
          @Override public String getObject() { return item.getModel().getObject().Name(); }
          @Override public void setObject(String Name) { item.getModel().getObject().Name(Name); }
        }).setEnabled(canEdit)).add(new TextField<String>("description", new IModel<String>() {
          private static final long serialVersionUID = -8595036776263395161L;
          @Override public String getObject() { return item.getModel().getObject().Description(); }
          @Override public void setObject(String Description) { item.getModel().getObject().Description(Description); }
        }).setEnabled(canEdit)).add(new Button("deleteButton") {
          private static final long serialVersionUID = 6357184943404360143L;
          @Override public void onSubmit() {
            if (null != item.getModel().getObject().FunctionalPermissionGroupID) { AuthorizationService.getInstance().deleteFunctionalPermissionGroup(item.getModel().getObject().FunctionalPermissionGroupID); }
          }
        }.setEnabled(canDelete).setVisible(canDelete)));
      }
    };
    add(plv);
    add(new PagingNavigator("functionalPermissionsGroupsListNavigator", plv));
  }
}

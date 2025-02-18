package software.theear.ui.auth;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiations;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
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
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.create}),
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.delete}),
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.edit})
})
public class FunctionalPermissionGroupOverviewPanel extends Panel {
  private final static String read = "readFunctionalPermissionGroup";
  private final static String create = "createFunctionalPermissionGroup";
  private final static String delete = "deleteFunctionalPermissionGroup";
  private final static String edit = "editFunctionalPermissionGroup";
  private static final long serialVersionUID = -6842093369143884412L;
  
  /** Model to manage functional permission groups.
   * 
   * The model just keeps the IDs of the functional permission groups and looks them up individually upon request. Subscriptions to {@link EBNewFunctionalPermissionGroup} and {@link EBDeletedFunctionalPermissionGroup} keep the list up to date.
   */
  private final static class FunctionalPermissionGroupModel implements IModel<List<FunctionalPermissionGroup>> {
    private static final long serialVersionUID = -992790658583847524L;

    private final static class FunctionalPermissionGroupList extends AbstractList<FunctionalPermissionGroup> implements Serializable {
      private static final long serialVersionUID = 3874351659133626347L;
      private final LinkedList<UUID> m_FunctionalPermissionGroupIDs = new LinkedList<>();
      
      private FunctionalPermissionGroupList() {
        // Lock to prevent concurrent modifications
        synchronized (this.m_FunctionalPermissionGroupIDs) {
          EBNewFunctionalPermissionGroup.GET.add(fpg -> { if (null != fpg) { synchronized (this.m_FunctionalPermissionGroupIDs) { if (!this.m_FunctionalPermissionGroupIDs.contains(fpg.FunctionalPermissionGroupID)) this.m_FunctionalPermissionGroupIDs.addLast(fpg.FunctionalPermissionGroupID); } } });
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

  /** Self reference to use in e.g. lambdas. */
  private final Panel m_THIS = this;
  
  public FunctionalPermissionGroupOverviewPanel(@Nonnull String id) {
    super(id);
    
    boolean canCreate = OidcUser.hasAllPermissions(this.getSession(), new String[] {create});
    boolean canEdit = OidcUser.hasAllPermissions(this.getSession(), new String[]{edit});
    boolean canDelete = OidcUser.hasAllPermissions(this.getSession(), new String[]{delete});
    
    // Build "new permission group form"
    Form<Void> newFunctionalPermissionGroupForm = new Form<>("newFunctionalPermissionGroupForm");
    
    Component createStatusLabel = new Label("createStatusLabel", Model.of("")).setOutputMarkupId(true);
    TextField<String> newFunctionalPermissionGroupNameInput = new TextField<>("createNameInput", Model.of("")); // TODO: add model that checks for duplicate names and shows warning!
    Component newButton = new Button("createButton") {
      private static final long serialVersionUID = 2275603002457204186L;

      @Override public void onSubmit() {
        if (null != AuthorizationService.getInstance().createFunctionalPermissionGroup(newFunctionalPermissionGroupNameInput.getValue(), "")) {
          newFunctionalPermissionGroupNameInput.setModel(Model.of(""));
          // TODO: display "success message"
        };
      }
    };
    
    newFunctionalPermissionGroupNameInput.add(new AjaxEventBehavior("input") {
      private static final long serialVersionUID = -3803444692177149506L;
      @Override protected void onEvent(AjaxRequestTarget target) {
        if (this.getComponent() instanceof TextField tf) {
          // Enable or disable button depending on name
          if (1 > tf.getInput().length()) {
            newButton.setEnabled(false);
            createStatusLabel.setDefaultModelObject(createStatusLabel.getString("duplicateName", createStatusLabel.getDefaultModel(), "The name is used for another functional permission group already."));
          } else if (tf.getInput().equalsIgnoreCase(tf.getValue())) {
            newButton.setEnabled(false);
            createStatusLabel.setDefaultModelObject("");
          } else if (AuthorizationService.getInstance().getAllFunctionalPermissionGroups().stream().anyMatch(e -> e.Name().equalsIgnoreCase(tf.getInput()))) {
            newButton.setEnabled(false);
            createStatusLabel.setDefaultModelObject(createStatusLabel.getString("emptyName", createStatusLabel.getDefaultModel(), "Functional permission group must have a valid name."));
          } else {
            newButton.setEnabled(canCreate);
            createStatusLabel.setDefaultModelObject("");
          }
          // Add components that require re-rendering
          target.add(newButton);
          target.add(createStatusLabel);
        }
      }
    });
    
    newFunctionalPermissionGroupForm.add(createStatusLabel);
    newFunctionalPermissionGroupForm.add(newFunctionalPermissionGroupNameInput);
    newFunctionalPermissionGroupForm.add(newButton);
    add(newFunctionalPermissionGroupForm);
    // Disable and hide for if user has insufficient permissions
    if (!canCreate) { newFunctionalPermissionGroupForm.setEnabled(false).setVisible(false); }
    
    PageableListView<FunctionalPermissionGroup> pageableFunctionalPermissionGroupListView = new PageableListView<>("functionalPermissionGroupsList", new FunctionalPermissionGroupModel(), 3) { // FIXME: make the "3" a parameter for the number of pages
      private static final long serialVersionUID = 1786621734119480353L;
      
      @Override protected void onConfigure() {
        super.onConfigure();
        this.setVisible(!this.getList().isEmpty());
      }

      @Override protected void populateItem(ListItem<FunctionalPermissionGroup> item) {
        Form<?> functionalPermissionGroupForm = new Form<>("functionalPermissionGroupsListForm");
        
        Component saveButton = new Button("saveUpdateButton") {
          private static final long serialVersionUID = -6940725764526359969L;
          @Override public void onSubmit() {
            // Update the functional permission group with the latest information in the form
            if (null != item.getModel().getObject().FunctionalPermissionGroupID) { AuthorizationService.getInstance().update(item.getModel().getObject()); }
          }
        }.setEnabled(false).setVisible(canEdit);
        
        Component deleteButton = new Button("deleteButton") {
          private static final long serialVersionUID = 6357184943404360143L;
          @Override public void onSubmit() {
            if (null != item.getModel().getObject().FunctionalPermissionGroupID) { AuthorizationService.getInstance().deleteFunctionalPermissionGroup(item.getModel().getObject().FunctionalPermissionGroupID); }
          }
        }.setDefaultFormProcessing(false).setEnabled(canDelete).setVisible(canDelete);
        
        Component localNotificationLabel = new Label("invalidName", Model.of("")).setOutputMarkupId(true);
        
        Component nameTF = new TextField<String>("name", new IModel<String>() {
          private static final long serialVersionUID = 7733493013501344701L;
          @Override public String getObject() { return item.getModel().getObject().Name(); }
          @Override public void setObject(String Name) { item.getModel().getObject().Name(Name); }
        }).add(new AjaxEventBehavior("input") { // Add behavior to react on FPG name change
          private static final long serialVersionUID = -3054092690482757698L;
          @Override protected void onEvent(AjaxRequestTarget target) {
            if (this.getComponent() instanceof TextField tf) {
              // Enable or disable "saveButton" depending on FPG name
              if (1 > tf.getInput().length()) {
                saveButton.setEnabled(false);
                localNotificationLabel.setDefaultModelObject(localNotificationLabel.getString("duplicateName", localNotificationLabel.getDefaultModel(), "The name is used for another functional permission group already."));
              } else if (tf.getInput().equalsIgnoreCase(tf.getValue())) {
                saveButton.setEnabled(false);
                localNotificationLabel.setDefaultModelObject("");
              } else if (AuthorizationService.getInstance().getAllFunctionalPermissionGroups().stream().anyMatch(e -> e.Name().equalsIgnoreCase(tf.getInput()))) {
                saveButton.setEnabled(false);
                localNotificationLabel.setDefaultModelObject(localNotificationLabel.getString("emptyName", localNotificationLabel.getDefaultModel(), "Functional permission group must have a valid name."));
              } else {
                saveButton.setEnabled(canEdit);
                localNotificationLabel.setDefaultModelObject("");
              }
              target.add(saveButton); // Add saveButton for "re-rendering"
              target.add(localNotificationLabel); // Add local notification label for re-rendering
            }
          }
        }).setEnabled(canEdit);
        
        Component descriptionTF = new TextField<String>("description", new IModel<String>() {
          private static final long serialVersionUID = -8595036776263395161L;
          @Override public String getObject() { return item.getModel().getObject().Description(); }
          @Override public void setObject(String Description) { item.getModel().getObject().Description(Description); }
        }).setEnabled(canEdit);
        item.add(new Link<String>("linkToDetailPanel") {
          private static final long serialVersionUID = 1L;
          @Override public void onClick() { m_THIS.replaceWith(new FunctionalPermissionGroupDetailsPanel(m_THIS.getId(), item.getModel().getObject())); }
        }).add(functionalPermissionGroupForm.add(nameTF).add(localNotificationLabel).add(descriptionTF).add(saveButton).add(deleteButton));
      }
    };
    add(pageableFunctionalPermissionGroupListView);
    add(new PagingNavigator("functionalPermissionsGroupsListNavigator", pageableFunctionalPermissionGroupListView));
  }
}

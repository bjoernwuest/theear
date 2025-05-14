package software.theear.ui.auth;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiations;
import org.apache.wicket.behavior.AttributeAppender;
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

import jakarta.annotation.Nonnull;
import software.theear.service.auth.AuthenticatedSession;
import software.theear.service.auth.AuthorizationService;
import software.theear.service.auth.EBDeletedFunctionalPermissionGroup;
import software.theear.service.auth.EBNewFunctionalPermissionGroup;
import software.theear.service.auth.FunctionalPermissionGroup;
import software.theear.service.auth.OidcUser;
import software.theear.ui.HomePage;
import software.theear.ui.HomePage.BackgroundJobFuture;
import software.theear.ui.WicketTimedJob;

@AuthorizeInstantiations(ruleset = {
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read}), 
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.create}),
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.delete}),
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.edit})
})
@SuppressWarnings("serial") public class FunctionalPermissionGroupOverviewPanel extends Panel {
  final static String read = "readFunctionalPermissionGroup";
  final static String create = "createFunctionalPermissionGroup";
  final static String delete = "deleteFunctionalPermissionGroup";
  final static String edit = "editFunctionalPermissionGroup";
  private static final long serialVersionUID = -6842093369143884412L;
  
  /** Model to manage functional permission groups.
   * 
   * The model just keeps the IDs of the functional permission groups and looks them up individually upon request. Subscriptions to {@link EBNewFunctionalPermissionGroup} and {@link EBDeletedFunctionalPermissionGroup} keep the list up to date.
   */
  private final static class FunctionalPermissionGroupModel implements IModel<List<FunctionalPermissionGroup>> {
    private final static class FunctionalPermissionGroupList extends AbstractList<FunctionalPermissionGroup> implements Serializable {
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
    TextField<String> newFunctionalPermissionGroupNameInput = new TextField<>("createNameInput", Model.of(""));
    Component newButton = new Button("createButton") {
      // Get reference to the button to be used in nested classes
      private final Button m_THIS = this;
      @Override public void onSubmit() {
        if (canCreate && m_THIS.getPage() instanceof HomePage hp) {
          // Disable button
          this.setEnabled(false);
          newFunctionalPermissionGroupNameInput.setEnabled(false);
          // Get name of functional permission group and reset text field
          String newGroupName = newFunctionalPermissionGroupNameInput.getValue();
          // Schedule functional permission group creation for background execution
          BackgroundJobFuture<Boolean> createFunctionalPermissionGroupInBackgroundJob = hp.runInBackground((Object... Args) -> {
            final UUID userID =  (Args[0] instanceof AuthenticatedSession as) ? as.getUser().UserID : new UUID(0, 0);
            if (null != AuthorizationService.getInstance().createFunctionalPermissionGroup(newGroupName, "", userID)) {
              // TODO: display "success message"
            };
            return false;
          }, m_THIS.getSession());
          // Register for AJAX timed job to reenable button
          hp.schedule(new WicketTimedJob() {
            @Override public void execute(AjaxRequestTarget Target, long Run) {
              // If the operation has been done, unregister this job and set button enablement status
              if (createFunctionalPermissionGroupInBackgroundJob.isDone()) {
                if (m_THIS.getPage() instanceof HomePage hp) { hp.unschedule(this); }
                newFunctionalPermissionGroupNameInput.setModel(Model.of(""));
                newFunctionalPermissionGroupNameInput.setEnabled(canCreate);
                m_THIS.setEnabled(canCreate);
                Target.add(m_THIS, newFunctionalPermissionGroupNameInput);
              }
            }
          });
        }
      }
    }.setEnabled(canCreate).setVisible(canCreate);
    // Status message and create button behavior depending on name of functional permission group
    newFunctionalPermissionGroupNameInput.add(new AjaxEventBehavior("input") {
      @Override protected void onEvent(AjaxRequestTarget target) {
        if (canCreate && this.getComponent() instanceof TextField tf) {
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
          target.add(newButton, createStatusLabel);
        }
      }
    }).setEnabled(canCreate).setVisible(canCreate);
    
    newFunctionalPermissionGroupForm.add(createStatusLabel, newFunctionalPermissionGroupNameInput, newButton).setEnabled(canCreate).setVisible(canCreate);
    add(newFunctionalPermissionGroupForm);
    
    PageableListView<FunctionalPermissionGroup> pageableFunctionalPermissionGroupListView = new PageableListView<>("functionalPermissionGroupsList", new FunctionalPermissionGroupModel(), 3) { // FIXME: make the "3" a parameter for the number of pages
      @Override protected void onConfigure() {
        super.onConfigure();
        this.setVisible(!this.getList().isEmpty());
      }

      @Override protected void populateItem(ListItem<FunctionalPermissionGroup> item) {
        item.add(new AttributeAppender("class", (item.getIndex() & 1) == 0 ? "even": "odd"));
        
        Form<?> functionalPermissionGroupForm = new Form<>("functionalPermissionGroupsListForm");
        
        Component saveButton = new Button("saveUpdateButton") { @Override public void onSubmit() { if (canEdit) { if (null != item.getModel().getObject().FunctionalPermissionGroupID) { AuthorizationService.getInstance().update(item.getModel().getObject()); } } } }.setEnabled(false).setVisible(canEdit);
        
        Component deleteButton = new Button("deleteButton") {
          @Override public void onSubmit() { if (canDelete) { if (null != item.getModel().getObject().FunctionalPermissionGroupID) { AuthorizationService.getInstance().deleteFunctionalPermissionGroup(item.getModel().getObject().FunctionalPermissionGroupID); } } }
        }.setDefaultFormProcessing(false).setEnabled(canDelete).setVisible(canDelete);
        
        Component localNotificationLabel = new Label("invalidName", Model.of("")).setOutputMarkupId(true);
        
        Component nameTF = new TextField<String>("name", new IModel<String>() {
          @Override public String getObject() { return item.getModel().getObject().Name(); }
          @Override public void setObject(String Name) { item.getModel().getObject().Name(Name); }
        }).add(new AjaxEventBehavior("input") { // Add behavior to react on FPG name change
          @Override protected void onEvent(AjaxRequestTarget target) {
            if (canEdit) {
              if (this.getComponent() instanceof TextField tf) {
                // Enable or disable "saveButton" depending on FPG name
                if (1 > tf.getInput().length()) {
                  saveButton.setEnabled(false);
                  localNotificationLabel.setDefaultModelObject(localNotificationLabel.getString("emptyName", localNotificationLabel.getDefaultModel(), "Functional permission group must have a valid name."));
                } else if (tf.getInput().equalsIgnoreCase(tf.getValue())) {
                  saveButton.setEnabled(canEdit); // Enable save of description
                  localNotificationLabel.setDefaultModelObject("");
                } else if (AuthorizationService.getInstance().getAllFunctionalPermissionGroups().stream().anyMatch(e -> e.Name().equalsIgnoreCase(tf.getInput()))) {
                  saveButton.setEnabled(false);
                  localNotificationLabel.setDefaultModelObject(localNotificationLabel.getString("duplicateName", localNotificationLabel.getDefaultModel(), "The name is used for another functional permission group already."));
                } else {
                  saveButton.setEnabled(canEdit);
                  localNotificationLabel.setDefaultModelObject("");
                }
                target.add(saveButton); // Add saveButton for "re-rendering"
                target.add(localNotificationLabel); // Add local notification label for re-rendering
              }
            }
          }
        }).setEnabled(canEdit);
        
        Component descriptionTF = new TextField<String>("description", new IModel<String>() {
          @Override public String getObject() { return item.getModel().getObject().Description(); }
          @Override public void setObject(String Description) { if (canEdit) { item.getModel().getObject().Description(Description); } }
        }).setEnabled(canEdit);
        
        item.add(new Link<String>("linkToDetailPanel") { @Override public void onClick() { m_THIS.replaceWith(new FunctionalPermissionGroupDetailsPanel(m_THIS.getId(), item.getModel().getObject().FunctionalPermissionGroupID)); } })
          .add(functionalPermissionGroupForm.add(nameTF, localNotificationLabel, descriptionTF, saveButton, deleteButton));
      }
    };
    add(pageableFunctionalPermissionGroupListView, new PagingNavigator("functionalPermissionsGroupsListNavigator", pageableFunctionalPermissionGroupListView));
  }
}

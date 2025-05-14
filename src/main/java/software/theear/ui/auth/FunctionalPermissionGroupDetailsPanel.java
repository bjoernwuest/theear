package software.theear.ui.auth;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiations;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
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
import software.theear.service.auth.FunctionalPermission;
import software.theear.service.auth.FunctionalPermissionGroup;
import software.theear.service.auth.OidcGroup;
import software.theear.service.auth.OidcUser;

@SuppressWarnings("serial")
@AuthorizeInstantiations(ruleset = {
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.delete}),
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.edit})
})
public class FunctionalPermissionGroupDetailsPanel extends Panel {
  private final FunctionalPermissionGroupDetailsPanel m_THIS;
  private final UUID m_FunctionalPermissionGroupID;

  public FunctionalPermissionGroupDetailsPanel(@Nonnull String id, @Nonnull UUID FunctionalPermissionGroupID) {
    super(id);
    this.m_THIS = this;
    this.m_FunctionalPermissionGroupID = FunctionalPermissionGroupID;
    
    // Subscribe as listener to react on deleted functional permission group and to replace this panel with overview panel
    EBDeletedFunctionalPermissionGroup.GET.add(fpg -> { if (fpg.FunctionalPermissionGroupID.equals(m_FunctionalPermissionGroupID)) { m_THIS.replaceWith(new FunctionalPermissionGroupOverviewPanel(m_THIS.getId())); } });
    
    boolean canEdit = OidcUser.hasAllPermissions(this.getSession(), new String[]{FunctionalPermissionGroupOverviewPanel.edit});
    boolean canDelete = OidcUser.hasAllPermissions(this.getSession(), new String[]{FunctionalPermissionGroupOverviewPanel.delete});
    
    Form<?> functionalPermissionGroupForm = new Form<>("functionalPermissionGroupForm");
    
    Component nameStatusLabel = new Label("nameStatusLabel", Model.of("")).setOutputMarkupId(true);
    
    Component saveButton = new Button("saveButton") {
      @Override public void onSubmit() {
        if (canEdit) {
          // Update the functional permission group with the latest information in the form
          AuthorizationService.getInstance().getFunctionalPermissionGroup(m_FunctionalPermissionGroupID).ifPresent(fpg -> AuthorizationService.getInstance().update(fpg));
          // FIXME: add notification upon successful save of changes
        }
      }
    }.setEnabled(false).setVisible(canEdit);
    
    Component deleteButton = new Button("deleteButton") {
      @Override public void onSubmit() {
        if (canDelete) {
          AuthorizationService.getInstance().deleteFunctionalPermissionGroup(m_FunctionalPermissionGroupID);
          m_THIS.replaceWith(new FunctionalPermissionGroupOverviewPanel(m_THIS.getId()));
        }
      }
    }.setDefaultFormProcessing(false).setEnabled(canDelete).setVisible(canDelete);
    
    Component nameTF = new TextField<String>("nameInput", new IModel<String>() {
      @Override public String getObject() {
        // The functional permission group may have been deleted already
        Optional<FunctionalPermissionGroup> fpg = AuthorizationService.getInstance().getFunctionalPermissionGroup(m_FunctionalPermissionGroupID);
        return fpg.map(f -> f.Name()).orElse("<deleted>");
      }
      @Override public void setObject(String Name) {
        // The functional permission group may have been deleted already
        Optional<FunctionalPermissionGroup> fpg = AuthorizationService.getInstance().getFunctionalPermissionGroup(m_FunctionalPermissionGroupID);
        fpg.ifPresent(f -> f.Name(Name));
      }
    }).add(new AjaxEventBehavior("input") { // Add behavior to react on FPG name change
      @Override protected void onEvent(AjaxRequestTarget target) {
        if (canEdit) {
          if (this.getComponent() instanceof TextField tf) {
            // Enable or disable "saveButton" depending on FPG name
            if (1 > tf.getInput().length()) {
              saveButton.setEnabled(false);
              nameStatusLabel.setDefaultModelObject(nameStatusLabel.getString("emptyName", nameStatusLabel.getDefaultModel(), "Functional permission group must have a valid name."));
            } else if (tf.getInput().equalsIgnoreCase(tf.getValue())) {
              saveButton.setEnabled(canEdit); // Enable save of description
              nameStatusLabel.setDefaultModelObject("");
            } else if (AuthorizationService.getInstance().getAllFunctionalPermissionGroups().stream().anyMatch(e -> e.Name().equalsIgnoreCase(tf.getInput()))) {
              saveButton.setEnabled(false);
              nameStatusLabel.setDefaultModelObject(nameStatusLabel.getString("duplicateName", nameStatusLabel.getDefaultModel(), "The name is used for another functional permission group already."));
            } else {
              saveButton.setEnabled(canEdit);
              nameStatusLabel.setDefaultModelObject("");
            }
            target.add(saveButton); // Add saveButton for "re-rendering"
            target.add(nameStatusLabel); // Add local notification label for re-rendering
          }
        }
      }
    }).setEnabled(canEdit);

    Component descriptionTF = new TextField<String>("description", new IModel<String>() {
      @Override public String getObject() {
        // The functional permission group may have been deleted already
        Optional<FunctionalPermissionGroup> fpg = AuthorizationService.getInstance().getFunctionalPermissionGroup(m_FunctionalPermissionGroupID);
        return fpg.map(f -> f.Description()).orElse("<deleted>");
      }
      @Override public void setObject(String Description) {
        // The functional permission group may have been deleted already
        Optional<FunctionalPermissionGroup> fpg = AuthorizationService.getInstance().getFunctionalPermissionGroup(m_FunctionalPermissionGroupID);
        fpg.ifPresent(f -> f.Description(Description));
      }
    }).setEnabled(canEdit);
    
    add(functionalPermissionGroupForm.add(nameStatusLabel, saveButton, deleteButton, nameTF, descriptionTF));
    
    PageableListView<UUID> pageableFunctionalPermissionsListView = new PageableListView<UUID>("functionalPermissions", AuthorizationService.getInstance().getAllFunctionalPermissions().stream().map(fp -> fp.FunctionalPermissionID).collect(Collectors.toList()), 3) { // FIXME: make the "3" configurable
      @Override protected void onConfigure() {
        super.onConfigure();
        this.setVisible(!this.getList().isEmpty());
      }

      @Override protected void populateItem(ListItem<UUID> item) {
        item.add(new AttributeAppender("class", (item.getIndex() & 1) == 0 ? "even": "odd"));
        
        item.add(new CheckBox("functionalPermissionAssigned", new IModel<Boolean>() {
          @Override public Boolean getObject() {
            Optional<FunctionalPermissionGroup> fpg = AuthorizationService.getInstance().getFunctionalPermissionGroup(m_FunctionalPermissionGroupID);
            if (!fpg.isPresent()) return false;
            return fpg.get().AssignedFunctionalPermissions.stream().anyMatch(fp -> fp.FunctionalPermissionID.equals(item.getModel().getObject()));
          }
        }).add(new AjaxEventBehavior("change") {
          @Override protected void onEvent(AjaxRequestTarget target) {
            if (this.getComponent() instanceof CheckBox cb) {
              // FIXME: add mechanism to listen on event to ensure that change was really reflected in data base; eventually (dis)enable component until event is received
              // FIXME: also add notification upon successful grant / revoke
              if (canEdit) {
                AuthorizationService.getInstance().getFunctionalPermissionGroup(m_FunctionalPermissionGroupID).ifPresent(fpg -> {
                  AuthorizationService.getInstance().getFunctionalPermission(item.getModel().getObject()).ifPresent(fp -> {
                    UUID userID = new UUID(0, 0);
                    if (Session.get() instanceof AuthenticatedSession as) userID = as.getUser().UserID;
                    if (null == cb.getInput()) { AuthorizationService.getInstance().revoke(fp, fpg, userID); }
                    else { AuthorizationService.getInstance().grant(fp, fpg, userID); }
                  });
                });
              }
            }
          }
        }).setEnabled(canEdit));
        item.add(new Label("functionalPermissionName", new IModel<String>() {
          @Override public String getObject() {
            Optional<FunctionalPermission> fp = AuthorizationService.getInstance().getFunctionalPermission(item.getModel().getObject());
            return fp.map(f -> f.PermissionName).orElse("<deleted>");
          }
        }));
        item.add(new Label("functionalPermissionDescription", new IModel<String>() {
          @Override public String getObject() {
            Optional<FunctionalPermission> fp = AuthorizationService.getInstance().getFunctionalPermission(item.getModel().getObject());
            return fp.map(f -> f.PermissionDescription).orElse("<deleted>");
          }
        }));
        item.add(new Label("functionalPermissionKnownSince", new IModel<String>() {
          @Override public String getObject() {
            Optional<FunctionalPermission> fp = AuthorizationService.getInstance().getFunctionalPermission(item.getModel().getObject());
            return fp.map(f -> f.CreatedAt.toString()).orElse("<deleted>");
          }
        }));
        item.add(new Label("functionalPermissionLastDetected", new IModel<String>() {
          @Override public String getObject() {
            Optional<FunctionalPermission> fp = AuthorizationService.getInstance().getFunctionalPermission(item.getModel().getObject());
            return fp.map(f -> f.LastSeenAt.toString()).orElse("<deleted>");
          }
        }));
      }
    };
    
    add(pageableFunctionalPermissionsListView, new PagingNavigator("functionalPermissionsListNavigator", pageableFunctionalPermissionsListView));
    
    // TODO: Manage assigned oidc roles
    PageableListView<OidcGroup> pageableOidcGroupsListView = new PageableListView<OidcGroup>("oidcGroups", AuthorizationService.getInstance().getOidcGroups(), 3) { // FIXME: make the "3" configurable
      @Override protected void onConfigure() {
        super.onConfigure();
        this.setVisible(!this.getList().isEmpty());
      }
      
      @Override protected void populateItem(ListItem<OidcGroup> item) {
        item.add(new AttributeAppender("class", (item.getIndex() & 1) == 0 ? "even": "odd"));
        item.add(new CheckBox("oidcGroupAssigned", new IModel<Boolean>() {
          @Override public Boolean getObject() {
            Optional<FunctionalPermissionGroup> fpg = AuthorizationService.getInstance().getFunctionalPermissionGroup(m_FunctionalPermissionGroupID);
            if (!fpg.isPresent()) return false;
            return fpg.get().getOidcGroups().stream().anyMatch(og -> og.equals(item.getModel().getObject()));
          }
        }).add(new AjaxEventBehavior("change") {
          @Override protected void onEvent(AjaxRequestTarget target) {
            if (this.getComponent() instanceof CheckBox cb) {
              // FIXME: add mechanism to listen on event to ensure that change was really reflected in data base; eventually (dis)enable component until event is received
              // FIXME: also add notification upon successful add / remove
              if (canEdit) {
                AuthorizationService.getInstance().getFunctionalPermissionGroup(m_FunctionalPermissionGroupID).ifPresent(fpg -> {
                  UUID userID = new UUID(0, 0);
                  if (Session.get() instanceof AuthenticatedSession as) userID = as.getUser().UserID;
                  if (null == cb.getInput()) AuthorizationService.getInstance().remove(item.getModel().getObject(), fpg, userID);
                  else AuthorizationService.getInstance().add(item.getModel().getObject(), fpg, userID);
                });
              }
            }
          }
        }).setEnabled(canEdit));
        item.add(new Label("oidcGroupIssuer", () -> item.getModel().getObject().Issuer));
        item.add(new Label("oidcGroupName", () -> item.getModel().getObject().GroupName));
        item.add(new Label("oidcGroupKnownSince", () -> item.getModel().getObject().CreatedAt));
      }
    };
    add(pageableOidcGroupsListView, new PagingNavigator("oidcGroupsListNavigator", pageableOidcGroupsListView));
  }
}

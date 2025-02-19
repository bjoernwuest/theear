package software.theear.ui.auth;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiations;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import jakarta.annotation.Nonnull;
import software.theear.service.auth.AuthorizationService;
import software.theear.service.auth.FunctionalPermission;
import software.theear.service.auth.FunctionalPermissionGroup;
import software.theear.service.auth.OidcUser;

@AuthorizeInstantiations(ruleset = {
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.delete}),
    @AuthorizeInstantiation({FunctionalPermissionGroupOverviewPanel.read, FunctionalPermissionGroupOverviewPanel.edit})
})
public class FunctionalPermissionGroupDetailsPanel extends Panel {
  private static final long serialVersionUID = -6403088453097327149L;
  private final FunctionalPermissionGroupDetailsPanel m_THIS;
  private final UUID m_FunctionalPermissionGroupID;

  public FunctionalPermissionGroupDetailsPanel(@Nonnull String id, @Nonnull UUID FunctionalPermissionGroupID) {
    super(id);
    this.m_THIS = this;
    this.m_FunctionalPermissionGroupID = FunctionalPermissionGroupID;
    
    boolean canEdit = OidcUser.hasAllPermissions(this.getSession(), new String[]{FunctionalPermissionGroupOverviewPanel.edit});
    boolean canDelete = OidcUser.hasAllPermissions(this.getSession(), new String[]{FunctionalPermissionGroupOverviewPanel.delete});
    
    Form<?> functionalPermissionGroupForm = new Form<>("functionalPermissionGroupForm");
    
    Component nameStatusLabel = new Label("nameStatusLabel", Model.of("")).setOutputMarkupId(true);
    
    Component saveButton = new Button("saveButton") {
      private static final long serialVersionUID = -6940725764526359969L;
      @Override public void onSubmit() {
        if (canEdit) {
          // Update the functional permission group with the latest information in the form
          AuthorizationService.getInstance().getFunctionalPermissionGroup(m_FunctionalPermissionGroupID).ifPresent(fpg -> AuthorizationService.getInstance().update(fpg));
        }
      }
    }.setEnabled(false).setVisible(canEdit);
    
    Component deleteButton = new Button("deleteButton") {
      private static final long serialVersionUID = 6357184943404360143L;
      @Override public void onSubmit() {
        if (canDelete) {
          AuthorizationService.getInstance().deleteFunctionalPermissionGroup(m_FunctionalPermissionGroupID);
          m_THIS.replaceWith(new FunctionalPermissionGroupOverviewPanel(m_THIS.getId()));
        }
      }
    }.setDefaultFormProcessing(false).setEnabled(canDelete).setVisible(canDelete);
    
    Component nameTF = new TextField<String>("nameInput", new IModel<String>() {
      private static final long serialVersionUID = 7733493013501344701L;
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
      private static final long serialVersionUID = -3054092690482757698L;
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
      private static final long serialVersionUID = -8595036776263395161L;
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

    
    // Functional permissions (with checkbox to assign / remove)
    // List assigned oidc roles?
    
    add(new ListView<UUID>("functionalPermissions", AuthorizationService.getInstance().getAllFunctionalPermissions().stream().map(fp -> fp.FunctionalPermissionID).collect(Collectors.toList())) {
      private static final long serialVersionUID = 7595827256754906532L;

      @Override protected void onConfigure() {
        super.onConfigure();
        this.setVisible(!this.getList().isEmpty());
      }

      @Override protected void populateItem(ListItem<UUID> item) {
        
        item.add(new CheckBox("functionalPermissionAssigned", new IModel<Boolean>() {
          private static final long serialVersionUID = 3569282844908858071L;
          @Override public Boolean getObject() {
            Optional<FunctionalPermissionGroup> fpg = AuthorizationService.getInstance().getFunctionalPermissionGroup(m_FunctionalPermissionGroupID);
            if (!fpg.isPresent()) return false;
            return fpg.get().AssignedFunctionalPermissions.stream().anyMatch(fp -> fp.FunctionalPermissionID.equals(item.getModel().getObject()));
          }
        }).add(new AjaxEventBehavior("change") {
          private static final long serialVersionUID = -262784779008523009L;
          @Override protected void onEvent(AjaxRequestTarget target) {
            if (this.getComponent() instanceof CheckBox cb) {
              if (canEdit) {
                AuthorizationService.getInstance().getFunctionalPermissionGroup(m_FunctionalPermissionGroupID).ifPresent(fpg -> {
                  AuthorizationService.getInstance().getFunctionalPermission(item.getModel().getObject()).ifPresent(fp -> {
                    if (null == cb.getInput()) { AuthorizationService.getInstance().revoke(fp, fpg); }
                    else { AuthorizationService.getInstance().grant(fp, fpg); }
                  });
                });
              }
            }
          }
        }).setEnabled(canEdit));
        item.add(new Label("functionalPermissionName", new IModel<String>() {
          private static final long serialVersionUID = -3384181086296704480L;
          @Override public String getObject() {
            Optional<FunctionalPermission> fp = AuthorizationService.getInstance().getFunctionalPermission(item.getModel().getObject());
            return fp.map(f -> f.PermissionName).orElse("<deleted>");
          }
        }));
        item.add(new Label("functionalPermissionDescription", new IModel<String>() {
          private static final long serialVersionUID = 129910192943448290L;
          @Override public String getObject() {
            Optional<FunctionalPermission> fp = AuthorizationService.getInstance().getFunctionalPermission(item.getModel().getObject());
            return fp.map(f -> f.PermissionDescription).orElse("<deleted>");
          }
        }));
        item.add(new Label("functionalPermissionKnownSince", new IModel<String>() {
          private static final long serialVersionUID = -8192719624897503491L;
          @Override public String getObject() {
            Optional<FunctionalPermission> fp = AuthorizationService.getInstance().getFunctionalPermission(item.getModel().getObject());
            return fp.map(f -> f.CreatedAt.toString()).orElse("<deleted>");
          }
        }));
        item.add(new Label("functionalPermissionLastDetected", new IModel<String>() {
          private static final long serialVersionUID = 4379228266737855484L;
          @Override public String getObject() {
            Optional<FunctionalPermission> fp = AuthorizationService.getInstance().getFunctionalPermission(item.getModel().getObject());
            return fp.map(f -> f.LastSeenAt.toString()).orElse("<deleted>");
          }
        }));
      }
    });
  }
}

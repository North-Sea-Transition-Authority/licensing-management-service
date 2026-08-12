package uk.co.nstauthority.licensingmanagementservice.topnavigation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.HashSet;
import java.util.Set;
import uk.co.nstauthority.licensingmanagementservice.document.search.DocumentTemplateSearchController;
import uk.co.nstauthority.licensingmanagementservice.licence.contact.LicenceContactController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleasePhase;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementController;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

public enum TopNavigationItem implements Displayable {

  WORK_AREA(
      "Work area",
      10,
      ReleasePhase.NOT_FLAGGED,
      ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))
  ),
  LICENCES(
      "Licences",
      20,
      ReleasePhase.LMS1,
      ReverseRouter.route(on(LicenceSearchController.class).renderSearchPage(null, null))
  ),
  TEAMS(
      "Teams",
      30,
      ReleasePhase.NOT_FLAGGED,
      ReverseRouter.route(on(TeamManagementController.class).renderTeamTypeList(null))
  ),
  LICENCE_CONTACTS(
      "Licence contacts",
      40,
      ReleasePhase.NOT_FLAGGED,
      ReverseRouter.route(on(LicenceContactController.class).renderManageContacts(null, null))
  ),
  DOCUMENT_LIBRARY(
      "Document library",
      50,
      ReleasePhase.LMS1,
      ReverseRouter.route(on(DocumentTemplateSearchController.class)
          .renderDocumentTemplateSearch(null, null, null)),
      TeamType.LICENCE_MANAGEMENT,
      new HashSet<>(TeamType.LICENCE_MANAGEMENT.getAllowedRoles())
  );

  private final String displayName;
  private final int displayOrder;
  private final ReleasePhase releasePhase;
  private final String url;
  private final TeamType requiredTeamType;
  private final Set<Role> requiredRoles;

  TopNavigationItem(String displayName, int displayOrder, ReleasePhase releasePhase, String url,
                    TeamType requiredTeamType, Set<Role> requiredRoles) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.releasePhase = releasePhase;
    this.url = url;
    this.requiredTeamType = requiredTeamType;
    this.requiredRoles = requiredRoles;
  }

  TopNavigationItem(String displayName, int displayOrder, ReleasePhase releasePhase, String url) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.releasePhase = releasePhase;
    this.url = url;
    this.requiredTeamType = null;
    this.requiredRoles = Set.of();
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  public ReleasePhase getReleasePhase() {
    return releasePhase;
  }

  public String getUrl() {
    return url;
  }

  public TeamType getRequiredTeamType() {
    return requiredTeamType;
  }

  public Set<Role> getRequiredRoles() {
    return requiredRoles;
  }
}

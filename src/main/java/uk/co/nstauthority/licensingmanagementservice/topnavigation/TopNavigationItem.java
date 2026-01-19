package uk.co.nstauthority.licensingmanagementservice.topnavigation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import uk.co.nstauthority.licensingmanagementservice.document.search.DocumentTemplateSearchController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementController;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

public enum TopNavigationItem {

  WORK_AREA("Work area",
      ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))
  ),
  LICENCES("Licences",
      ReverseRouter.route(on(LicenceSearchController.class).renderSearchPage(null))
  ),
  TEAMS("Teams",
      ReverseRouter.route(on(TeamManagementController.class).renderTeamTypeList(null))
  ),
  DOCUMENT_LIBRARY(
      "Document library",
      ReverseRouter.route(on(DocumentTemplateSearchController.class)
          .renderDocumentTemplateSearch(null, null, null, null))
  );

  private final String displayName;
  private final String url;

  TopNavigationItem(
      String displayName,
      String url
  ) {
    this.displayName = displayName;
    this.url = url;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getUrl() {
    return url;
  }
}
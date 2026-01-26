package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.management.ScopedTeamManagementController;

public enum TeamType {

  LICENCE_MANAGEMENT(
      "Licence management",
      "licence-management",
      false,
      false,
      List.of(
          Role.MANAGE_TEAM,
          Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM,
          Role.OFFLINE_LICENCE_ADMINISTRATOR,
          Role.SCHEDULE_ADMINISTRATOR,
          Role.WORK_PROGRAMME_ADMINISTRATOR,
          Role.LICENCE_SCHEDULE_WORK_PROGRAMME_VIEWER,
          Role.DOCUMENT_TEMPLATE_MANAGER
     ),
      null
  ),
  OFFSHORE_PRODUCTION_LICENSING(
      "Offshore production licensing",
      "offshore-production-licensing",
      false,
      false,
      List.of(
          Role.MANAGE_TEAM,
          Role.CASE_MANAGER_NEW_VENTURES,
          Role.CASE_MANAGER_OPERATIONS,
          Role.STEWARD_NEW_VENTURES,
          Role.STEWARD_OPERATIONS,
          Role.DECISION_ISSUER_NEW_VENTURES,
          Role.DECISION_ISSUER_OPERATIONS,
          Role.CONTINUATION_REVIEWER_NEW_VENTURES,
          Role.CONTINUATION_REVIEWER_OPERATIONS
      ),
      null
  ),
  PRODUCTION(
      "Production",
      "production",
      false,
      false,
      List.of(Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE),
      null
  ),
  CARBON_STORAGE(
      "Carbon storage",
      "carbon-storage",
      false,
      false,
      List.of(Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE),
      null
  ),
  ORGANISATION(
      "Organisations",
      "organisation",
      true,
      false,
      List.of(Role.MANAGE_TEAM, Role.APPLICATION_EDITOR, Role.APPLICATION_SUBMITTER, Role.VIEW_ORGANISATION_LICENCES),
          () ->
              ReverseRouter.route(on(ScopedTeamManagementController.class).renderCreateNewOrgTeam(null))
  ),
  EXTERNAL_CONTRIBUTORS(
      "External contributors",
      "external-contributors",
      true,
      true,
      List.of(Role.MANAGE_TEAM, Role.EXTERNAL_APPLICATION_EDITOR, Role.EXTERNAL_APPLICATION_VIEWER),
      null
  );

  private final String displayName;
  private final String urlSlug;
  private final boolean isScoped;
  private final boolean isApplicationScoped;
  private final List<Role> allowedRoles;
  private final Supplier<String> createNewInstanceRoute;

  TeamType(
      String displayName,
      String urlSlug,
      boolean isScoped,
      boolean isApplicationScoped,
      List<Role> allowedRoles,
      Supplier<String> createNewInstanceRoute
  ) {
    this.displayName = displayName;
    this.urlSlug = urlSlug;
    this.isScoped = isScoped;
    this.isApplicationScoped = isApplicationScoped;
    this.allowedRoles = allowedRoles;
    this.createNewInstanceRoute = createNewInstanceRoute;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getUrlSlug() {
    return urlSlug;
  }

  public boolean isScoped() {
    return isScoped;
  }

  public boolean isApplicationScoped() {
    return isApplicationScoped;
  }

  public List<Role> getAllowedRoles() {
    return allowedRoles;
  }

  public String getCreateNewInstanceRoute() {
    return createNewInstanceRoute.get();
  }

  public static Optional<TeamType> fromUrlSlug(String urlSlug) {
    return Arrays.stream(values())
        .filter(teamType -> teamType.urlSlug.equals(urlSlug))
        .findFirst();
  }

}
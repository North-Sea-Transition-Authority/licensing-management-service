package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.management.ScopedTeamManagementController;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum TeamType implements Displayable {

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
          Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR,
          Role.LICENCE_SCHEDULE_WORK_PROGRAMME_VIEWER,
          Role.DOCUMENT_TEMPLATE_MANAGER
     ),
      null,
      true
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
      null,
      true
  ),
  ONSHORE_PRODUCTION_LICENSING(
      "Onshore production licensing",
      "onshore-production-licensing",
      false,
      false,
      List.of(
          Role.MANAGE_TEAM,
          Role.CASE_MANAGER_ONSHORE,
          Role.STEWARD_ONSHORE,
          Role.DECISION_ISSUER_ONSHORE
      ),
      null,
      true
  ),
  CARBON_STORAGE_LICENSING(
      "Carbon Storage Licensing",
      "carbon-storage-licensing",
      false,
      false,
      List.of(
          Role.MANAGE_TEAM,
          Role.CASE_MANAGER_CS_NEW_VENTURES,
          Role.CASE_MANAGER_CS_CTS,
          Role.STEWARD_CS_NEW_VENTURES,
          Role.STEWARD_CS_CTS,
          Role.DECISION_ISSUER_CS_NEW_VENTURES,
          Role.DECISION_ISSUER_CS_CTS
      ),
      null,
      true
  ),
  REGULATIONS_LICENSING(
      "Regulations Licensing",
      "regulations-licensing",
      false,
      false,
      List.of(
          Role.MANAGE_TEAM,
          Role.CONTINUATION_ISSUER,
          Role.DECISION_EXECUTOR
      ),
      null,
      true
  ),
  ORGANISATION(
      "Organisations",
      "organisation",
      true,
      false,
      List.of(Role.MANAGE_TEAM, Role.APPLICATION_EDITOR, Role.APPLICATION_SUBMITTER, Role.VIEW_ORGANISATION_LICENCES),
          () ->
              ReverseRouter.route(on(ScopedTeamManagementController.class).renderCreateNewOrgTeam(null)),
      false
  ),
  EXTERNAL_CONTRIBUTORS(
      "External contributors",
      "external-contributors",
      true,
      true,
      List.of(Role.EXTERNAL_APPLICATION_EDITOR, Role.EXTERNAL_APPLICATION_VIEWER),
      null,
      false
  );

  private final String displayName;
  private final String urlSlug;
  private final boolean isScoped;
  private final boolean isApplicationScoped;
  private final List<Role> allowedRoles;
  private final Supplier<String> createNewInstanceRoute;
  private final boolean isRegulator;

  TeamType(
      String displayName,
      String urlSlug,
      boolean isScoped,
      boolean isApplicationScoped,
      List<Role> allowedRoles,
      Supplier<String> createNewInstanceRoute,
      boolean isRegulator
  ) {
    this.displayName = displayName;
    this.urlSlug = urlSlug;
    this.isScoped = isScoped;
    this.isApplicationScoped = isApplicationScoped;
    this.allowedRoles = allowedRoles;
    this.createNewInstanceRoute = createNewInstanceRoute;
    this.isRegulator = isRegulator;
  }

  @Override
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

  public boolean isRegulator() {
    return isRegulator;
  }

  public static Optional<TeamType> fromUrlSlug(String urlSlug) {
    return Arrays.stream(values())
        .filter(teamType -> teamType.urlSlug.equals(urlSlug))
        .findFirst();
  }
}
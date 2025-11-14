package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.management.ScopedTeamManagementController;

public enum TeamType {

  LICENCE_MAINTENANCE(
      "Licence maintenance",
      "licence-maintenance",
      false,
      List.of(Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE, Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM),
      null
  ),
  PRODUCTION(
      "Production",
      "production",
      false,
      List.of(Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE),
      null
  ),
  CARBON_STORAGE(
      "Carbon storage",
      "carbon-storage",
      false,
      List.of(Role.MANAGE_TEAM, Role.VIEW_ANY_LICENCE),
      null
  ),
  ORGANISATION(
      "Organisations",
      "organisation",
      true,
      List.of(Role.MANAGE_TEAM, Role.APPLICATION_EDITOR, Role.APPLICATION_SUBMITTER, Role.VIEW_ORGANISATION_LICENCES),
          () ->
              ReverseRouter.route(on(ScopedTeamManagementController.class).renderCreateNewOrgTeam(null))
  );

  private final String displayName;
  private final String urlSlug;
  private final boolean isScoped;
  private final List<Role> allowedRoles;
  private final Supplier<String> createNewInstanceRoute;

  TeamType(
      String displayName,
      String urlSlug,
      boolean isScoped,
      List<Role> allowedRoles,
      Supplier<String> createNewInstanceRoute
  ) {
    this.displayName = displayName;
    this.urlSlug = urlSlug;
    this.isScoped = isScoped;
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

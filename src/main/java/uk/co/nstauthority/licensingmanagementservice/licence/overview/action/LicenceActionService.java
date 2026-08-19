package uk.co.nstauthority.licensingmanagementservice.licence.overview.action;

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionRoles;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicenceTimelinePositionTab;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTab;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTab;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;

@Service
public class LicenceActionService {

  private final RegisteredActions registeredActions;
  private final TeamQueryService teamQueryService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceCorrectionService licenceCorrectionService;
  private final Environment environment;
  private final LicenceStatusService licenceStatusService;

  public LicenceActionService(
      TeamQueryService teamQueryService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceCorrectionService licenceCorrectionService,
      Environment environment,
      LicenceStatusService licenceStatusService
  ) {
    this.teamQueryService = teamQueryService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceCorrectionService = licenceCorrectionService;
    this.environment = environment;
    this.licenceStatusService = licenceStatusService;

    this.registeredActions = LicenceActionBuilder.newBuilder()
        .registerAction(LicenceActionItem.CREATE_LICENCE_SCHEDULE)
          .requiresAnyRoleFrom(Role.SCHEDULE_ADMINISTRATOR)
          .requiresAnyStatus()
          .positionWithinTabs(LicenceScheduleTab.class)
          .requiresAnyTypeFrom(LicenceType.CARBON_STORAGE, LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION)
          .withLicenceScheduleRequirement(LicenceScheduleRequirement.NO_SCHEDULE_EXISTS)
          .isPrimaryButton(false)
        .registerAction(LicenceActionItem.UPDATE_LICENCE_SCHEDULE)
          .requiresAnyRoleFrom(Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR)
          .requiresAnyStatus()
          .positionWithinTabs(LicenceScheduleTab.class)
          .requiresAnyTypeFrom(LicenceType.CARBON_STORAGE, LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION)
          .withLicenceScheduleRequirement(LicenceScheduleRequirement.CAN_CREATE_DRAFT)
          .isPrimaryButton(false)
        .registerAction(LicenceActionItem.EDIT_LICENCE_DETAILS)
          .requiresAnyRoleFrom(Role.OFFLINE_LICENCE_ADMINISTRATOR)
          .requiresAnyStatus()
          .positionAtTopLevel()
          .requiresAnyTypeManagedByLms()
          .withoutLicenceScheduleRequirement()
          .isPrimaryButton(false)
        .registerAction(LicenceActionItem.START_CORRECTION)
          .requiresAnyRoleFrom(Role.PRODUCTION_LICENCE_CORRECTOR, Role.CARBON_STORAGE_LICENCE_CORRECTOR)
        .requiresAnyStatus()
          .positionWithinTabs(LicenceTimelinePositionTab.class)
          .requiresAnyTypeFrom(LicenceType.CARBON_STORAGE, LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION)
          .withoutLicenceScheduleRequirement()
          .isPrimaryButton(false)
        .build();
  }

  public List<ActionItemView> getTopLevelLicenceActionItems(Licence licence, ServiceUserDetail user) {
    return filterLicenceActionItems(licence, registeredActions.topLevelLicenceActionItems(), user);
  }

  public List<ActionItemView> getLicenceActionItemsForTab(Licence licence, ServiceUserDetail user, LicenceTab licenceTab) {
    var actionItems = registeredActions.getLicenceActionItemsForTab(licenceTab);
    return filterLicenceActionItems(licence, actionItems, user);
  }

  public List<ActionItemView> getAvailableUserActionItems(Licence licence, ServiceUserDetail user) {
    return filterLicenceActionItems(licence, EnumSet.allOf(LicenceActionItem.class), user);
  }

  private List<ActionItemView> filterLicenceActionItems(
      Licence licence,
      Collection<LicenceActionItem> licenceActionItems,
      ServiceUserDetail user
  ) {
    if (licenceActionItems.isEmpty()) {
      return List.of();
    }

    var userRoles = teamQueryService.getTeamRolesForUser(user.wuaId()).stream().map(TeamRole::getRole).collect(toSet());
    var licenceScheduleState = getLicenceScheduleState(licence);
    var currentStatus = licenceStatusService.getCurrentStatus(licence);

    return licenceActionItems
        .stream()
        // remove the actions which aren't applicable to the current licence status
        .filter(action -> registeredActions.isLicenceApplicableToStatus(action, currentStatus))
        // remove actions that users can't see given their roles
        .filter(action -> registeredActions.canRolesAccessAction(action, userRoles))
        // remove the actions which aren't applicable to the licence type
        .filter(action -> registeredActions.isActionApplicableToLicenceType(action, licence))
        // remove the actions which do not meet the licence schedule requirement
        .filter(action -> satisfiesLicenceScheduleRequirement(
            licenceScheduleState,
            registeredActions.licenceScheduleRequirementMap().get(action)
        ))
        .filter(action -> !LicenceActionItem.START_CORRECTION.equals(action) || canStartCorrection(licence, userRoles))
        .map(action -> action.toActionItemView(licence, registeredActions.isPrimary(action, licence)))
        .sorted(Comparator.comparing(ActionItemView::displayOrder))
        .toList();
  }

  private record LicenceScheduleState(boolean anyScheduleExists, boolean activeScheduleExists, boolean openDraftExists) {}

  private LicenceScheduleState getLicenceScheduleState(Licence licence) {
    var statuses = licenceScheduleDetailService.getAllScheduleDetailsByLicence(licence).stream()
        .map(LicenceScheduleDetail::getStatus)
        .filter(status -> !LicenceScheduleDetailStatus.DELETED.equals(status))
        .collect(toSet());

    return new LicenceScheduleState(
        !statuses.isEmpty(),
        statuses.contains(LicenceScheduleDetailStatus.ACTIVE),
        statuses.contains(LicenceScheduleDetailStatus.DRAFT)
    );
  }

  private boolean satisfiesLicenceScheduleRequirement(
      LicenceScheduleState licenceScheduleState,
      Set<LicenceScheduleRequirement> requirements
  ) {
    if (requirements.contains(LicenceScheduleRequirement.NO_REQUIREMENT)) {
      return true;
    }

    if (requirements.contains(LicenceScheduleRequirement.NO_SCHEDULE_EXISTS)) {
      return !licenceScheduleState.anyScheduleExists;
    }

    return licenceScheduleState.activeScheduleExists && !licenceScheduleState.openDraftExists;
  }

  private boolean canStartCorrection(Licence licence, Set<Role> userRoles) {
    if (!environment.acceptsProfiles(Profiles.of("enable-lms2"))) {
      return false;
    }
    if (licenceCorrectionService.hasOpenCorrection(licence)) {
      return false;
    }
    return LicenceCorrectionRoles.getRequiredRoleForLicenceType(licence.getType())
        .map(userRoles::contains)
        .orElse(false);
  }
}
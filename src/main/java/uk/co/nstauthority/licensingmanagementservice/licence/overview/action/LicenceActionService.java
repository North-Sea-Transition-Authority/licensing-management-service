package uk.co.nstauthority.licensingmanagementservice.licence.overview.action;

import static java.util.stream.Collectors.toSet;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;

@Service
public class LicenceActionService {
  private static final Map<LicenceActionItem, Set<Role>> ACTIONS_TO_ROLES
      = new EnumMap<>(LicenceActionItem.class);
  private static final Map<LicenceStatus, Set<LicenceActionItem>> STATUS_TO_ACTIONS
      = new EnumMap<>(LicenceStatus.class);
  private static final Map<LicenceActionItem, Set<LicenceType>> ACTIONS_TO_LICENCE_TYPE
      = new EnumMap<>(LicenceActionItem.class);
  private static final Map<LicenceActionItem, Set<LicenceScheduleRequirement>> ACTIONS_TO_LICENCE_SCHEDULE_REQUIREMENT
      = new EnumMap<>(LicenceActionItem.class);
  private static final Map<LicenceActionItem, Predicate<Licence>> ACTIONS_TO_PRIMARY_PREDICATES
      = new EnumMap<>(LicenceActionItem.class);

  private final TeamQueryService teamQueryService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;

  public LicenceActionService(
      TeamQueryService teamQueryService,
      LicenceScheduleDetailService licenceScheduleDetailService
  ) {
    this.teamQueryService = teamQueryService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;

    var registeredActions = LicenceActionBuilder.newBuilder()
        .registerAction(LicenceActionItem.CREATE_LICENCE_SCHEDULE)
          .requiresAnyRoleFrom(Role.SCHEDULE_ADMINISTRATOR)
          .requiresAnyStatus()
          .requiresAnyTypeFrom(LicenceType.CARBON_STORAGE, LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION)
          .withLicenceScheduleRequirement(LicenceScheduleRequirement.NO_SCHEDULE_EXISTS)
          .isPrimaryButton(false)
        .registerAction(LicenceActionItem.UPDATE_LICENCE_SCHEDULE)
          .requiresAnyRoleFrom(Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR)
          .requiresAnyStatus()
          .requiresAnyTypeFrom(LicenceType.CARBON_STORAGE, LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION)
          .withLicenceScheduleRequirement(LicenceScheduleRequirement.CAN_CREATE_DRAFT)
          .isPrimaryButton(false)
        .registerAction(LicenceActionItem.MANAGE_LICENSEES)
          .requiresAnyRoleFrom(Role.OFFLINE_LICENCE_ADMINISTRATOR)
          .requiresAnyStatus()
          .requiresAnyTypeManagedByLms()
          .withoutLicenceScheduleRequirement()
          .isPrimaryButton(false)
        .registerAction(LicenceActionItem.MANAGE_RESPONSIBLE_TEAM)
          .requiresAnyRoleFrom(Role.OFFLINE_LICENCE_ADMINISTRATOR)
          .requiresAnyStatus()
          .requiresAnyTypeFrom(LicenceType.CARBON_STORAGE)
          .withoutLicenceScheduleRequirement()
          .isPrimaryButton(false)
        .build();

    ACTIONS_TO_ROLES.putAll(registeredActions.roleMap);
    STATUS_TO_ACTIONS.putAll(registeredActions.statusMap);
    ACTIONS_TO_LICENCE_TYPE.putAll(registeredActions.licenceTypeMap);
    ACTIONS_TO_LICENCE_SCHEDULE_REQUIREMENT.putAll(registeredActions.licenceScheduleRequirementMap);
    ACTIONS_TO_PRIMARY_PREDICATES.putAll(registeredActions.primaryActionPredicateMap);
  }

  public List<ActionItemView> getAvailableUserActionItems(
      Licence licence,
      ServiceUserDetail user
  ) {
    var userRoles = teamQueryService.getTeamRolesForUser(user.wuaId()).stream()
            .map(TeamRole::getRole)
            .collect(toSet());

    return EnumSet.allOf(LicenceActionItem.class).stream()
        // remove the actions which aren't applicable to the current licence status
        .filter(STATUS_TO_ACTIONS.get(licence.getStatus())::contains)
        // remove actions that users can't see given their roles
        .filter(action -> CollectionUtils.containsAny(ACTIONS_TO_ROLES.get(action), userRoles))
        // remove the actions which aren't applicable to the licence type
        .filter(action -> ACTIONS_TO_LICENCE_TYPE.get(action).contains(licence.getType()))
        // remove the actions which do not meet the licence schedule requirement
        .filter(action -> satisfiesLicenceScheduleRequirement(licence, ACTIONS_TO_LICENCE_SCHEDULE_REQUIREMENT.get(action)))
        .map(actionItem -> actionItem.toActionItemView(licence, isPrimary(licence, actionItem)))
        .sorted(Comparator.comparing(ActionItemView::displayOrder))
        .toList();
  }

  private boolean satisfiesLicenceScheduleRequirement(
      Licence licence,
      Set<LicenceScheduleRequirement> requirements
  ) {
    if (requirements.contains(LicenceScheduleRequirement.NO_REQUIREMENT)) {
      return true;
    }

    var existingNonDeletedScheduleDetails = licenceScheduleDetailService.getAllScheduleDetailsByLicence(licence).stream()
        .filter(detail -> !detail.getStatus().equals(LicenceScheduleDetailStatus.DELETED))
        .toList();

    if (requirements.contains(LicenceScheduleRequirement.NO_SCHEDULE_EXISTS)) {
      return existingNonDeletedScheduleDetails.isEmpty();
    }

    var activeScheduleExists = existingNonDeletedScheduleDetails.stream()
        .anyMatch(detail -> detail.getStatus().equals(LicenceScheduleDetailStatus.ACTIVE));

    var openDraftExists = existingNonDeletedScheduleDetails.stream()
        .anyMatch(detail -> detail.getStatus().equals(LicenceScheduleDetailStatus.DRAFT));

    return activeScheduleExists && !openDraftExists;
  }

  private static boolean isPrimary(Licence licence, LicenceActionItem actionItem) {
    return ACTIONS_TO_PRIMARY_PREDICATES.getOrDefault(actionItem, l -> false)
        .test(licence);
  }
}

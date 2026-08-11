package uk.co.nstauthority.licensingmanagementservice.licence.overview.action;

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
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

  private static final Map<LicenceActionItem, Set<Role>> ACTIONS_TO_ROLES
      = new EnumMap<>(LicenceActionItem.class);
  private static final Map<LicenceStatusType, Set<LicenceActionItem>> STATUS_TO_ACTIONS
      = new EnumMap<>(LicenceStatusType.class);
  private static final Map<LicenceActionItem, Set<LicenceType>> ACTIONS_TO_LICENCE_TYPE
      = new EnumMap<>(LicenceActionItem.class);
  private static final Map<LicenceActionItem, Set<LicenceScheduleRequirement>> ACTIONS_TO_LICENCE_SCHEDULE_REQUIREMENT
      = new EnumMap<>(LicenceActionItem.class);
  private static final Map<LicenceActionItem, Predicate<Licence>> ACTIONS_TO_PRIMARY_PREDICATES
      = new EnumMap<>(LicenceActionItem.class);
  private static final Set<LicenceActionItem> TOP_LEVEL_LICENCE_ACTION_ITEMS
      = new HashSet<>();
  private static final Map<Class<? extends LicenceTab>, Set<LicenceActionItem>> LICENCE_ACTION_ITEMS_BY_LICENCE_TAB_CLASS
      = new HashMap<>();

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

    var registeredActions = LicenceActionBuilder.newBuilder()
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
          .requiresAnyRole()//TODO - LMS2-55: Define who can carry out corrections on a licence
          .requiresAnyStatus()
          .positionWithinTabs(LicenceTimelinePositionTab.class)
          .requiresAnyTypeFrom(LicenceType.CARBON_STORAGE, LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION)
          .withoutLicenceScheduleRequirement()
          .isPrimaryButton(false)
        .build();

    ACTIONS_TO_ROLES.putAll(registeredActions.roleMap);
    STATUS_TO_ACTIONS.putAll(registeredActions.statusMap);
    ACTIONS_TO_LICENCE_TYPE.putAll(registeredActions.licenceTypeMap);
    ACTIONS_TO_LICENCE_SCHEDULE_REQUIREMENT.putAll(registeredActions.licenceScheduleRequirementMap);
    ACTIONS_TO_PRIMARY_PREDICATES.putAll(registeredActions.primaryActionPredicateMap);
    TOP_LEVEL_LICENCE_ACTION_ITEMS.addAll(registeredActions.topLevelLicenceActionItems);
    LICENCE_ACTION_ITEMS_BY_LICENCE_TAB_CLASS.putAll(registeredActions.licenceActionItemsByLicenceTabClass);
  }

  public List<ActionItemView> getTopLevelLicenceActionItems(Licence licence, ServiceUserDetail user) {
    return filterLicenceActionItems(licence, TOP_LEVEL_LICENCE_ACTION_ITEMS, user);
  }

  public List<ActionItemView> getLicenceActionItemsForTab(Licence licence, ServiceUserDetail user, LicenceTab licenceTab) {
    var actionItems = LICENCE_ACTION_ITEMS_BY_LICENCE_TAB_CLASS.getOrDefault(licenceTab.getClass(), Set.of());
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

    var userRoles = teamQueryService.getTeamRolesForUser(user.wuaId()).stream()
        .map(TeamRole::getRole)
        .collect(toSet());

    var licenceScheduleState = getLicenceScheduleState(licence);
    var currentStatus = licenceStatusService.getCurrentStatus(licence);

    return licenceActionItems.stream()
        // remove the actions which aren't applicable to the current licence status
        .filter(STATUS_TO_ACTIONS.get(currentStatus)::contains)
        // remove actions that users can't see given their roles
        .filter(action -> CollectionUtils.containsAny(ACTIONS_TO_ROLES.get(action), userRoles))
        // remove the actions which aren't applicable to the licence type
        .filter(action -> ACTIONS_TO_LICENCE_TYPE.get(action).contains(licence.getType()))
        // remove the actions which do not meet the licence schedule requirement
        .filter(action -> satisfiesLicenceScheduleRequirement(
            licenceScheduleState,
            ACTIONS_TO_LICENCE_SCHEDULE_REQUIREMENT.get(action))
        )
        .filter(action -> !LicenceActionItem.START_CORRECTION.equals(action) || canStartCorrection(licence))
        .map(actionItem -> actionItem.toActionItemView(licence, isPrimary(licence, actionItem)))
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

  private boolean canStartCorrection(Licence licence) {
    if (!environment.acceptsProfiles(Profiles.of("enable-lms2"))) {
      return false;
    }
    return !licenceCorrectionService.hasOpenCorrection(licence);
  }

  private static boolean isPrimary(Licence licence, LicenceActionItem actionItem) {
    return ACTIONS_TO_PRIMARY_PREDICATES.getOrDefault(actionItem, l -> false)
        .test(licence);
  }
}

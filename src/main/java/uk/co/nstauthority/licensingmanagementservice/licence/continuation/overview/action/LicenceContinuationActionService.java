package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.function.SingletonSupplier;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;

@Service
public class LicenceContinuationActionService {
  private static final Map<LicenceContinuationActionItem, Set<Role>> ACTIONS_TO_ROLES
      = new EnumMap<>(LicenceContinuationActionItem.class);
  private static final Map<ApplicationStatus,
      Set<LicenceContinuationActionItem>> STATUS_TO_ACTIONS
      = new EnumMap<>(ApplicationStatus.class);
  private static final Set<LicenceContinuationActionItem> ACTIONS_ALLOWED_WHEN_DRAFT_UPDATE_OPEN
      = EnumSet.noneOf(LicenceContinuationActionItem.class);
  private static final Map<LicenceContinuationActionItem,
      Predicate<LicenceContinuationApplicationDetail>> ACTIONS_TO_PRIMARY_PREDICATES =
      new EnumMap<>(LicenceContinuationActionItem.class);
  private final TeamQueryService teamQueryService;
  private final LicenceContinuationApplicationDetailRepository detailRepository;

  public LicenceContinuationActionService(
      TeamQueryService teamQueryService,
      LicenceContinuationApplicationDetailRepository detailRepository
  ) {
    this.teamQueryService = teamQueryService;
    this.detailRepository = detailRepository;

    var registeredActions = LicenceContinuationActionBuilder.newBuilder()
        .registerAction(LicenceContinuationActionItem.CONFIRM_CONTINUATION)
          .requiresAnyRoleFrom(RegulatorRoleService.CONTINUATION_REVIEWER_ROLES)
          .requiresAnyStatusFrom(ApplicationStatus.SUBMITTED)
          .isPrimaryButton(true)
          .allowWhenDraftUpdateOpen(false)
        .registerAction(LicenceContinuationActionItem.WITHDRAW_CONTINUATION)
          .requiresAnyRoleFrom(RegulatorRoleService.CONTINUATION_REVIEWER_ROLES)
          .requiresAnyStatusFrom(ApplicationStatus.SUBMITTED)
          .allowWhenDraftUpdateOpen(true)
        .registerAction(LicenceContinuationActionItem.REQUEST_UPDATE)
          .requiresAnyRoleFrom(RegulatorRoleService.CONTINUATION_REVIEWER_ROLES)
          .requiresAnyStatusFrom(ApplicationStatus.SUBMITTED)
          .allowWhenDraftUpdateOpen(false)
        .build();

    ACTIONS_TO_ROLES.putAll(registeredActions.roleMap);
    STATUS_TO_ACTIONS.putAll(registeredActions.statusMap);
    ACTIONS_TO_PRIMARY_PREDICATES.putAll(registeredActions.primaryActionPredicateMap);
    ACTIONS_ALLOWED_WHEN_DRAFT_UPDATE_OPEN.addAll(registeredActions.draftUpdateOpenActions);
  }

  public List<ActionItemView> getAvailableUserActionItems(
      LicenceContinuationApplicationDetail applicationDetail,
      ServiceUserDetail user
  ) {
    var userRoles = teamQueryService.getTeamRolesForUser(user.wuaId()).stream()
        .map(TeamRole::getRole)
        .collect(Collectors.toSet());

    var hasLaterVersion = SingletonSupplier.of(() -> hasLaterVersion(applicationDetail));

    return EnumSet.allOf(LicenceContinuationActionItem.class).stream()
        .filter(STATUS_TO_ACTIONS.getOrDefault(applicationDetail.getStatus(), Set.of())::contains)
        .filter(action -> CollectionUtils.containsAny(ACTIONS_TO_ROLES.get(action), userRoles))
        .filter(action -> ACTIONS_ALLOWED_WHEN_DRAFT_UPDATE_OPEN.contains(action) || !hasLaterVersion.get())
        .map(actionItem -> actionItem.toActionItemView(applicationDetail, isPrimary(applicationDetail, actionItem)))
        .sorted(Comparator.comparing(ActionItemView::displayOrder))
        .toList();
  }

  private boolean hasLaterVersion(LicenceContinuationApplicationDetail applicationDetail) {
    return detailRepository.existsByLicenceContinuationApplicationAndVersionNumberGreaterThanAndStatusNot(
        applicationDetail.getLicenceContinuationApplication(),
        applicationDetail.getVersionNumber(),
        ApplicationStatus.DELETED
    );
  }

  private static boolean isPrimary(
      LicenceContinuationApplicationDetail applicationDetail,
      LicenceContinuationActionItem actionItem
  ) {
    return ACTIONS_TO_PRIMARY_PREDICATES.getOrDefault(actionItem, detail -> false)
        .test(applicationDetail);
  }
}

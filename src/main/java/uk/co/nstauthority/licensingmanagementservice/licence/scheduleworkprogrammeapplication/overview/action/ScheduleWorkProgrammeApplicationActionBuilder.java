package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action;

import java.util.Arrays;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;

public class ScheduleWorkProgrammeApplicationActionBuilder {

  static RegisterAnAction newBuilder() {
    return new Builder();
  }

  static class Builder implements SetRolesForAnAction, SetStatusForAnAction, RegisterAnAction {
    public final Map<ScheduleWorkProgrammeApplicationStatus, Set<ScheduleWorkProgrammeApplicationActionItem>> statusMap =
        new EnumMap<>(ScheduleWorkProgrammeApplicationStatus.class);
    public final Map<ScheduleWorkProgrammeApplicationActionItem, Set<Role>> roleMap =
        new EnumMap<>(ScheduleWorkProgrammeApplicationActionItem.class);
    public final Map<ScheduleWorkProgrammeApplicationActionItem,
        Function<ScheduleWorkProgrammeApplicationDetail, Long>> userGrantPredicateMap =
        new EnumMap<>(ScheduleWorkProgrammeApplicationActionItem.class);
    private final Deque<ScheduleWorkProgrammeApplicationActionItem> actionItems = new LinkedList<>();
    public final Map<ScheduleWorkProgrammeApplicationActionItem,
        Predicate<ScheduleWorkProgrammeApplicationDetail>> primaryActionPredicateMap
        = new EnumMap<>(ScheduleWorkProgrammeApplicationActionItem.class);

    private Builder() {
    }

    @Override
    public SetStatusForAnAction registerAction(ScheduleWorkProgrammeApplicationActionItem actionItem) {
      if (actionItems.contains(actionItem)) {
        throw new IllegalStateException("You cannot register %s as it has already been registered".formatted(actionItem));
      }

      this.actionItems.push(actionItem);
      return this;
    }

    @Override
    public RegisterAnAction requiresAnyRoleFrom(Role... roles) {
      roleMap.put(actionItems.peek(), Arrays.stream(roles).collect(Collectors.toSet()));
      return this;
    }

    @Override
    public SetRolesForAnAction requiresAnyStatusFrom(ScheduleWorkProgrammeApplicationStatus... statuses) {
      for (ScheduleWorkProgrammeApplicationStatus status : statuses) {
        statusMap.merge(
            status,
            Set.of(Objects.requireNonNull(actionItems.peek())),
            Builder::merge
        );
      }
      return this;
    }

    @Override
    public RegisterAnAction orGrantedToUser(
        Function<ScheduleWorkProgrammeApplicationDetail, Long> userExtractor) {
      userGrantPredicateMap.put(actionItems.peek(), userExtractor);
      return this;
    }

    @Override
    public SetRolesForAnAction requiresAnyStatus() {
      var statuses = Arrays.stream(ScheduleWorkProgrammeApplicationStatus.values()).toList();
      for (ScheduleWorkProgrammeApplicationStatus status : statuses) {
        statusMap.merge(
            status,
            Set.of(Objects.requireNonNull(actionItems.peek())),
            Builder::merge
        );
      }
      return this;
    }

    @Override
    public RegisterAnAction isPrimaryButton(Predicate<ScheduleWorkProgrammeApplicationDetail> condition) {
      primaryActionPredicateMap.put(actionItems.peek(), condition);
      return this;
    }

    @Override
    public RegisterAnAction isPrimaryButton(boolean isPrimary) {
      return isPrimaryButton(detail -> isPrimary);
    }

    @Override
    public Builder build() {
      var missingActions = CollectionUtils.disjunction(
          actionItems,
          Arrays.stream(ScheduleWorkProgrammeApplicationActionItem.values()).toList()
      );
      if (!CollectionUtils.isEmpty(missingActions)) {
        throw new IllegalStateException("Missing registration for following actions %s".formatted(missingActions));
      }
      return this;
    }

    private static <T> Set<T> merge(Set<T> a, Set<T> b) {
      var mergedSet = new HashSet<>(a);
      mergedSet.addAll(b);
      return mergedSet;
    }
  }

  interface SetStatusForAnAction {
    SetRolesForAnAction requiresAnyStatusFrom(ScheduleWorkProgrammeApplicationStatus... statuses);

    SetRolesForAnAction requiresAnyStatus();
  }

  interface SetRolesForAnAction {
    RegisterAnAction requiresAnyRoleFrom(Role... roles);
  }

  interface RegisterAnAction {
    SetStatusForAnAction registerAction(ScheduleWorkProgrammeApplicationActionItem actionItem);

    RegisterAnAction isPrimaryButton(Predicate<ScheduleWorkProgrammeApplicationDetail> condition);

    RegisterAnAction isPrimaryButton(boolean isPrimary);

    RegisterAnAction orGrantedToUser(Function<ScheduleWorkProgrammeApplicationDetail, Long> userExtractor);

    Builder build();
  }
}

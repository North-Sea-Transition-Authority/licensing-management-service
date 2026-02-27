package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action;

import java.util.Arrays;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
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
    private final Deque<ScheduleWorkProgrammeApplicationActionItem> actionItems = new LinkedList<>();

    private Builder() {
    }

    @Override
    public SetRolesForAnAction registerAction(ScheduleWorkProgrammeApplicationActionItem actionItem) {
      if (actionItems.contains(actionItem)) {
        throw new IllegalStateException("You cannot register %s as it has already been registered".formatted(actionItem));
      }

      this.actionItems.push(actionItem);
      return this;
    }

    @Override
    public SetStatusForAnAction requiresAnyRoleFrom(Role... roles) {
      roleMap.put(actionItems.peek(), Arrays.stream(roles).collect(Collectors.toSet()));
      return this;
    }

    @Override
    public RegisterAnAction requiresAnyStatusFrom(ScheduleWorkProgrammeApplicationStatus... statuses) {
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
    public RegisterAnAction requiresAnyStatus() {
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

  interface SetRolesForAnAction {
    SetStatusForAnAction requiresAnyRoleFrom(Role... roles);
  }

  interface SetStatusForAnAction {
    RegisterAnAction requiresAnyStatusFrom(ScheduleWorkProgrammeApplicationStatus... statuses);

    RegisterAnAction requiresAnyStatus();
  }

  interface RegisterAnAction {
    SetRolesForAnAction registerAction(ScheduleWorkProgrammeApplicationActionItem actionItem);

    Builder build();
  }
}

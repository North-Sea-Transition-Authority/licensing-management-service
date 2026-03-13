package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action;

import java.util.Arrays;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;

public class LicenceContinuationActionBuilder {

  static RegisterAnAction newBuilder() {
    return new Builder();
  }

  static class Builder implements SetRolesForAnAction, SetStatusForAnAction, RegisterAnAction {
    public final Map<LicenceContinuationApplicationStatus, Set<LicenceContinuationActionItem>> statusMap =
        new EnumMap<>(LicenceContinuationApplicationStatus.class);
    public final Map<LicenceContinuationActionItem, Set<Role>> roleMap =
        new EnumMap<>(LicenceContinuationActionItem.class);
    private final Deque<LicenceContinuationActionItem> actionItems = new LinkedList<>();

    private Builder() {
    }

    @Override
    public SetRolesForAnAction registerAction(LicenceContinuationActionItem actionItem) {
      if (actionItems.contains(actionItem)) {
        throw new IllegalStateException("You cannot register %s as it has already been registered".formatted(actionItem));
      }

      this.actionItems.push(actionItem);
      return this;
    }

    @Override
    public SetStatusForAnAction requiresAnyRoleFrom(Set<Role> roles) {
      roleMap.put(actionItems.peek(), roles);
      return this;
    }

    @Override
    public RegisterAnAction requiresAnyStatusFrom(LicenceContinuationApplicationStatus... statuses) {
      for (LicenceContinuationApplicationStatus status : statuses) {
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
      var statuses = Arrays.stream(LicenceContinuationApplicationStatus.values()).toList();
      for (LicenceContinuationApplicationStatus status : statuses) {
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
          Arrays.stream(LicenceContinuationActionItem.values()).toList()
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
    SetStatusForAnAction requiresAnyRoleFrom(Set<Role> roles);
  }

  interface SetStatusForAnAction {
    RegisterAnAction requiresAnyStatusFrom(LicenceContinuationApplicationStatus... statuses);

    RegisterAnAction requiresAnyStatus();
  }

  interface RegisterAnAction {
    SetRolesForAnAction registerAction(LicenceContinuationActionItem actionItem);

    Builder build();
  }
}

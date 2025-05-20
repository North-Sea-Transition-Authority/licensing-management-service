package uk.co.nstauthority.template.xyzapplication.processing.action;

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
import uk.co.nstauthority.template.teams.Role;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationStatus;

public class RegisterAndSetPermissionsForAnActionBuilder {

  static RegisterAnAction newBuilder() {
    return new Builder();
  }

  static class Builder implements SetRolesForAnAction, SetStatusForAnAction, RegisterAnAction {
    public final Map<XyzApplicationStatus, Set<CaseProcessingActionItem>> statusMap =
        new EnumMap<>(XyzApplicationStatus.class);
    public final Map<CaseProcessingActionItem, Set<Role>> roleMap =
        new EnumMap<>(CaseProcessingActionItem.class);
    private final Deque<CaseProcessingActionItem> actionItems = new LinkedList<>();

    private Builder() {
    }

    @Override
    public SetRolesForAnAction registerAction(CaseProcessingActionItem actionItem) {
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
    public RegisterAnAction requiresAnyStatusFrom(XyzApplicationStatus... statuses) {

      for (XyzApplicationStatus status : statuses) {
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
          Arrays.stream(CaseProcessingActionItem.values()).toList()
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
    RegisterAnAction requiresAnyStatusFrom(XyzApplicationStatus... statuses);
  }

  interface RegisterAnAction {
    SetRolesForAnAction registerAction(CaseProcessingActionItem actionItem);

    Builder build();
  }
}

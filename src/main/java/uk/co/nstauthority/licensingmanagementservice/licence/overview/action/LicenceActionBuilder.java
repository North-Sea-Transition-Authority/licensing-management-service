package uk.co.nstauthority.licensingmanagementservice.licence.overview.action;

import java.util.Arrays;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;

public class LicenceActionBuilder {

  static RegisterAnAction newBuilder() {
    return new Builder();
  }

  static class Builder implements
      SetRolesForAnAction,
      SetStatusForAnAction,
      SetLicenceTypeForAnAction,
      SetLicenceScheduleRequirementForAnAction,
      RegisterAnAction {

    public final Map<LicenceStatusType, Set<LicenceActionItem>> statusMap =
        new EnumMap<>(LicenceStatusType.class);
    public final Map<LicenceActionItem, Set<Role>> roleMap =
        new EnumMap<>(LicenceActionItem.class);
    public final Map<LicenceActionItem, Set<LicenceType>> licenceTypeMap =
        new EnumMap<>(LicenceActionItem.class);
    public final Map<LicenceActionItem, Set<LicenceScheduleRequirement>> licenceScheduleRequirementMap =
        new EnumMap<>(LicenceActionItem.class);
    private final Deque<LicenceActionItem> actionItems = new LinkedList<>();
    public final Map<LicenceActionItem, Predicate<Licence>> primaryActionPredicateMap =
        new EnumMap<>(LicenceActionItem.class);

    private Builder() {
    }

    @Override
    public SetRolesForAnAction registerAction(LicenceActionItem actionItem) {
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
    public SetStatusForAnAction requiresAnyRole() {
      roleMap.put(actionItems.peek(), Arrays.stream(Role.values()).collect(Collectors.toSet()));
      return this;
    }

    @Override
    public SetLicenceTypeForAnAction requiresAnyStatusFrom(LicenceStatusType... statuses) {

      for (LicenceStatusType status : statuses) {
        statusMap.merge(
            status,
            Set.of(Objects.requireNonNull(actionItems.peek())),
            Builder::merge
        );
      }

      return this;
    }

    @Override
    public SetLicenceTypeForAnAction requiresAnyStatus() {
      var statuses = Arrays.stream(LicenceStatusType.values()).toList();

      for (LicenceStatusType status : statuses) {
        statusMap.merge(
            status,
            Set.of(Objects.requireNonNull(actionItems.peek())),
            Builder::merge
        );
      }

      return this;
    }

    @Override
    public SetLicenceScheduleRequirementForAnAction requiresAnyTypeFrom(LicenceType... licenceTypes) {
      licenceTypeMap.put(actionItems.peek(), Arrays.stream(licenceTypes).collect(Collectors.toSet()));
      return this;
    }

    @Override
    public SetLicenceScheduleRequirementForAnAction requiresAnyTypeManagedByLms() {
      licenceTypeMap.put(actionItems.peek(), new HashSet<>(LicenceType.getLicenceTypesManagedByLms()));
      return this;
    }

    @Override
    public SetLicenceScheduleRequirementForAnAction requiresAnyType() {
      licenceTypeMap.put(actionItems.peek(), Arrays.stream(LicenceType.values()).collect(Collectors.toSet()));
      return this;
    }

    @Override
    public RegisterAnAction withLicenceScheduleRequirement(LicenceScheduleRequirement licenceScheduleRequirement) {
      licenceScheduleRequirementMap.put(actionItems.peek(), Set.of(licenceScheduleRequirement));
      return this;
    }

    @Override
    public RegisterAnAction withoutLicenceScheduleRequirement() {
      licenceScheduleRequirementMap.put(actionItems.peek(), Set.of(LicenceScheduleRequirement.NO_REQUIREMENT));
      return this;
    }

    @Override
    public RegisterAnAction isPrimaryButton(Predicate<Licence> condition) {
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
          Arrays.stream(LicenceActionItem.values()).toList()
      );
      if (!CollectionUtils.isEmpty(missingActions)) {
        throw new IllegalStateException("Missing registration for following actions %s".formatted(missingActions));
      }
      return this;
    }

    private static <T> Set<T> merge(
        Set<T> a,
        Set<T> b
    ) {
      var mergedSet = new HashSet<>(a);
      mergedSet.addAll(b);
      return mergedSet;
    }
  }

  interface SetRolesForAnAction {
    SetStatusForAnAction requiresAnyRoleFrom(Role... roles);

    SetStatusForAnAction requiresAnyRole();
  }

  interface SetStatusForAnAction {
    SetLicenceTypeForAnAction requiresAnyStatusFrom(LicenceStatusType... statuses);

    SetLicenceTypeForAnAction requiresAnyStatus();
  }

  interface SetLicenceTypeForAnAction {
    SetLicenceScheduleRequirementForAnAction requiresAnyTypeFrom(LicenceType... licenceTypes);

    SetLicenceScheduleRequirementForAnAction requiresAnyTypeManagedByLms();

    SetLicenceScheduleRequirementForAnAction requiresAnyType();
  }

  interface SetLicenceScheduleRequirementForAnAction {
    RegisterAnAction withLicenceScheduleRequirement(LicenceScheduleRequirement licenceScheduleRequirement);

    RegisterAnAction withoutLicenceScheduleRequirement();
  }

  interface RegisterAnAction {
    SetRolesForAnAction registerAction(LicenceActionItem actionItem);

    RegisterAnAction isPrimaryButton(Predicate<Licence> condition);

    RegisterAnAction isPrimaryButton(boolean isPrimary);

    Builder build();
  }
}

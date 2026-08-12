package uk.co.nstauthority.licensingmanagementservice.licence.overview.action;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.collections4.CollectionUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTab;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;

public record RegisteredActions(
    Map<LicenceStatusType, Set<LicenceActionItem>> statusMap,
    Map<LicenceActionItem, Set<Role>> roleMap,
    Map<LicenceActionItem, Set<LicenceType>> licenceTypeMap,
    Map<LicenceActionItem, Set<LicenceScheduleRequirement>> licenceScheduleRequirementMap,
    Map<LicenceActionItem, Predicate<Licence>> primaryActionPredicateMap,
    Set<LicenceActionItem> topLevelLicenceActionItems,
    Map<Class<? extends LicenceTab>, Set<LicenceActionItem>> licenceActionItemsByLicenceTabClass
) {

  public boolean isPrimary(LicenceActionItem licenceActionItem, Licence licence) {
    return primaryActionPredicateMap.getOrDefault(licenceActionItem, l -> false).test(licence);
  }

  public Set<LicenceActionItem> getLicenceActionItemsForTab(LicenceTab licenceTab) {
    return licenceActionItemsByLicenceTabClass.getOrDefault(licenceTab.getClass(), Set.of());
  }

  public boolean isLicenceApplicableToStatus(LicenceActionItem licenceActionItem, LicenceStatusType licenceStatusType) {
    return statusMap.getOrDefault(licenceStatusType, Set.of()).contains(licenceActionItem);
  }

  public boolean canRolesAccessAction(LicenceActionItem licenceActionItem, Collection<Role> roles) {
    return CollectionUtils.containsAny(roleMap.getOrDefault(licenceActionItem, Set.of()), roles);
  }

  public boolean isActionApplicableToLicenceType(LicenceActionItem licenceActionItem, Licence licence) {
    return licenceTypeMap.get(licenceActionItem).contains(licence.getType());
  }

}

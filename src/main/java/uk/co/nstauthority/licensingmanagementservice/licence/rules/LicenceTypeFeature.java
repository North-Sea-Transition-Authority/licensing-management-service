package uk.co.nstauthority.licensingmanagementservice.licence.rules;

import jakarta.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTypeGroup;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;

public enum LicenceTypeFeature {
  TERMS(EnumSet.of(
      LicenceTypeGroup.PRODUCTION,
      LicenceTypeGroup.CARBON_STORAGE,
      LicenceTypeGroup.GAS_STORAGE,
      LicenceTypeGroup.EXPLORATION,
      LicenceTypeGroup.METHANE_DRAINAGE
  )),
  PHASES(EnumSet.of(
      LicenceTypeGroup.PRODUCTION,
      LicenceTypeGroup.GAS_STORAGE
  )),
  PHASES_CAPTURED(EnumSet.of(
      LicenceTypeGroup.PRODUCTION,
      LicenceTypeGroup.GAS_STORAGE
  )),
  RENTAL_RATES(EnumSet.of(
      LicenceTypeGroup.PRODUCTION,
      LicenceTypeGroup.EXPLORATION,
      LicenceTypeGroup.METHANE_DRAINAGE
  )),
  RENTAL_RATES_EXPONENTIAL(EnumSet.of(
      LicenceTypeGroup.PRODUCTION
  )),
  RENTAL_RATES_FLAT(EnumSet.of(
      LicenceTypeGroup.EXPLORATION,
      LicenceTypeGroup.METHANE_DRAINAGE
  )),
  WORK_PROGRAMMES(EnumSet.of(
      LicenceTypeGroup.PRODUCTION,
      LicenceTypeGroup.CARBON_STORAGE,
      LicenceTypeGroup.GAS_STORAGE
  )),
  WORK_PROGRAMMES_PHASE_TIED(EnumSet.of(
      LicenceTypeGroup.PRODUCTION
  )),
  WORK_PROGRAMMES_TERM_TIED(EnumSet.of(
      LicenceTypeGroup.CARBON_STORAGE
  ), EnumSet.of(
      TermType.APPRAISAL
  )),
  CAN_CREATE_SCHEDULE_WORK_PROGRAMME_APPLICATIONS(EnumSet.of(
      LicenceTypeGroup.PRODUCTION,
      LicenceTypeGroup.CARBON_STORAGE
  )),
  ;

  private final Set<LicenceType> supportingLicenceTypes;
  private final Set<TermType> supportingTermTypes;

  LicenceTypeFeature(Set<LicenceTypeGroup> supportingLicenceTypes, Set<TermType> supportingTermTypes) {
    this.supportingLicenceTypes = LicenceTypeGroup.flattenLicenceTypeGroups(supportingLicenceTypes);
    this.supportingTermTypes = supportingTermTypes;
  }

  LicenceTypeFeature(Set<LicenceTypeGroup> supportingLicenceTypes) {
    this(supportingLicenceTypes, Set.of());
  }

  public boolean isEnabledFor(LicenceType licenceType, @Nullable TermType termType) {
    if (!supportingLicenceTypes.contains(licenceType)) {
      return false;
    }
    if (supportingTermTypes.isEmpty()) {
      return true;
    }
    return termType != null && supportingTermTypes.contains(termType);
  }

  public boolean isEnabledFor(LicenceType licenceType) {
    return isEnabledFor(licenceType, null);
  }
}

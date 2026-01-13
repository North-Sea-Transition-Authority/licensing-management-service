package uk.co.nstauthority.licensingmanagementservice.licence.rules;

import jakarta.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;

public enum LicenceTypeFeature {
  TERMS(EnumSet.of(
      LicenceType.LANDWARD_PRODUCTION,
      LicenceType.SEAWARD_PRODUCTION,
      LicenceType.CARBON_STORAGE,
      LicenceType.GAS_STORAGE,
      LicenceType.LANDWARD_EXPLORATION,
      LicenceType.SEAWARD_EXPLORATION,
      LicenceType.METHANE_DRAINAGE
  )),
  PHASES(EnumSet.of(
      LicenceType.SEAWARD_PRODUCTION,
      LicenceType.GAS_STORAGE
  )),
  PHASES_CAPTURED(EnumSet.of(
      LicenceType.SEAWARD_PRODUCTION,
      LicenceType.GAS_STORAGE
  )),
  RENTAL_RATES(EnumSet.of(
      LicenceType.LANDWARD_PRODUCTION,
      LicenceType.SEAWARD_PRODUCTION,
      LicenceType.LANDWARD_EXPLORATION,
      LicenceType.SEAWARD_EXPLORATION,
      LicenceType.METHANE_DRAINAGE
  )),
  RENTAL_RATES_EXPONENTIAL(EnumSet.of(
      LicenceType.LANDWARD_PRODUCTION,
      LicenceType.SEAWARD_PRODUCTION
  )),
  RENTAL_RATES_FLAT(EnumSet.of(
      LicenceType.LANDWARD_EXPLORATION,
      LicenceType.SEAWARD_EXPLORATION,
      LicenceType.METHANE_DRAINAGE
  )),
  WORK_PROGRAMMES(EnumSet.of(
      LicenceType.LANDWARD_PRODUCTION,
      LicenceType.SEAWARD_PRODUCTION,
      LicenceType.CARBON_STORAGE,
      LicenceType.GAS_STORAGE
  )),
  WORK_PROGRAMMES_PHASE_TIED(EnumSet.of(
      LicenceType.SEAWARD_PRODUCTION
  )),
  WORK_PROGRAMMES_TERM_TIED(EnumSet.of(
      LicenceType.CARBON_STORAGE
  ), EnumSet.of(
      TermType.APPRAISAL
  )),
  CAN_CREATE_SCHEDULE_WORK_PROGRAMME_APPLICATIONS(EnumSet.of(
      LicenceType.LANDWARD_PRODUCTION,
      LicenceType.SEAWARD_PRODUCTION,
      LicenceType.CARBON_STORAGE
  )),
  SHOW_ROUND_ISSUED_ON(EnumSet.of(
      LicenceType.LANDWARD_PRODUCTION,
      LicenceType.SEAWARD_PRODUCTION
  ))
  ;

  private final Set<LicenceType> supportingLicenceTypes;
  private final Set<TermType> supportingTermTypes;

  LicenceTypeFeature(Set<LicenceType> supportingLicenceTypes, Set<TermType> supportingTermTypes) {
    this.supportingLicenceTypes = supportingLicenceTypes;
    this.supportingTermTypes = supportingTermTypes;
  }

  LicenceTypeFeature(Set<LicenceType> supportingLicenceTypes) {
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

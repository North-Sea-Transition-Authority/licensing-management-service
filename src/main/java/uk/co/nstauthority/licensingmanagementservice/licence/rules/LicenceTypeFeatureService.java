package uk.co.nstauthority.licensingmanagementservice.licence.rules;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.EventType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;

@Service
public class LicenceTypeFeatureService implements LicenceTypeRulesResolver {
  @Override
  public boolean hasTerms(LicenceType licenceType) {
    return LicenceTypeFeature.TERMS.isEnabledFor(licenceType);
  }

  @Override
  public Set<TermType> getTerms(LicenceType licenceType) {
    if (!hasTerms(licenceType)) {
      return Set.of();
    }

    return TermType.getTermsFor(licenceType);
  }

  @Override
  public boolean hasPhases(LicenceType licenceType) {
    return LicenceTypeFeature.PHASES.isEnabledFor(licenceType);
  }

  @Override
  public Set<PhaseType> getPhases(LicenceType licenceType, TermType termType) {
    if (!hasPhases(licenceType)) {
      return Set.of();
    }

    return PhaseType.getPhasesFor(termType);
  }

  @Override
  public boolean arePhasesCaptured(LicenceType licenceType) {
    return LicenceTypeFeature.PHASES_CAPTURED.isEnabledFor(licenceType);
  }

  @Override
  public boolean hasRentalRate(LicenceType licenceType) {
    return LicenceTypeFeature.RENTAL_RATES.isEnabledFor(licenceType);
  }

  @Override
  public boolean isRentalRatesFlat(LicenceType licenceType) {
    return LicenceTypeFeature.RENTAL_RATES_FLAT.isEnabledFor(licenceType);
  }

  @Override
  public boolean isRentalRatesExponential(LicenceType licenceType) {
    return LicenceTypeFeature.RENTAL_RATES_EXPONENTIAL.isEnabledFor(licenceType);
  }

  @Override
  public boolean hasWorkProgramme(LicenceType licenceType) {
    return LicenceTypeFeature.WORK_PROGRAMMES.isEnabledFor(licenceType);
  }

  @Override
  public boolean isWorkProgrammesTermTied(LicenceType licenceType, TermType termType) {
    return LicenceTypeFeature.WORK_PROGRAMMES_TERM_TIED.isEnabledFor(licenceType, termType);
  }

  @Override
  public boolean isWorkProgrammesPhaseTied(LicenceType licenceType) {
    return LicenceTypeFeature.WORK_PROGRAMMES_PHASE_TIED.isEnabledFor(licenceType);
  }

  @Override
  public Set<EventType> getSupportedEvents(LicenceType licenceType) {
    return EventType.getEventsFor(licenceType);
  }

  @Override
  public List<LicenceType> getLicenceTypesThatCanCreateScheduleWorkProgrammeApplications() {
    return Arrays.stream(LicenceType.values())
        .filter(LicenceTypeFeature.CAN_CREATE_SCHEDULE_WORK_PROGRAMME_APPLICATIONS::isEnabledFor)
        .toList();
  }
}

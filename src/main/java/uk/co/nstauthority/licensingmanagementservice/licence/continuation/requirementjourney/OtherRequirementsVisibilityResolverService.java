package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Service
public class OtherRequirementsVisibilityResolverService {

  private static final Set<PhaseType> FINANCIAL_CAPACITY_TARGET_PHASES = Set.of(PhaseType.PHASE_C);
  private static final Set<TermType> FINANCIAL_CAPACITY_TARGET_TERMS = Set.of(TermType.SECOND, TermType.THIRD);
  private static final Set<TermType> RELINQUISHMENT_TARGET_TERMS = Set.of(TermType.SECOND);
  private static final Set<TermType> DEVELOPMENT_CONSENT_TARGET_TERMS = Set.of(TermType.THIRD);

  private final LicenceScheduleService licenceScheduleService;

  public OtherRequirementsVisibilityResolverService(LicenceScheduleService licenceScheduleService) {
    this.licenceScheduleService = licenceScheduleService;
  }

  public OtherRequirementsVisibility resolve(LicenceScheduleDetail licenceScheduleDetail) {
    var nextPhase = licenceScheduleService.getNextPhase(licenceScheduleDetail);

    var nextTerm = nextPhase.isEmpty()
                   ? licenceScheduleService.getNextTerm(licenceScheduleDetail)
                   : Optional.ofNullable(licenceScheduleService.getCurrentTerm(licenceScheduleDetail));

    var phaseType = nextPhase.map(LicenceSchedulePhase::getPhaseType).orElse(null);
    var termType = nextTerm.map(LicenceScheduleTerm::getTermType).orElse(null);

    var showFinancial = (phaseType != null && FINANCIAL_CAPACITY_TARGET_PHASES.contains(phaseType))
                            || (termType != null && FINANCIAL_CAPACITY_TARGET_TERMS.contains(termType));

    var showRelinquishment = termType != null && RELINQUISHMENT_TARGET_TERMS.contains(termType);

    var showDevelopmentConsent = termType != null && DEVELOPMENT_CONSENT_TARGET_TERMS.contains(termType);

    return new OtherRequirementsVisibility(
        showFinancial,
        showRelinquishment,
        showDevelopmentConsent
    );
  }
}
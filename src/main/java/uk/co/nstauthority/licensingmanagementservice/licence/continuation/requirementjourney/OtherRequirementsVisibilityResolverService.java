package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class OtherRequirementsVisibilityResolverService {

  private static final Set<PhaseType> FINANCIAL_CAPACITY_TARGET_PHASES = Set.of(PhaseType.PHASE_C);
  private static final Set<TermType> FINANCIAL_CAPACITY_TARGET_TERMS = Set.of(TermType.SECOND, TermType.THIRD);
  private static final Set<TermType> RELINQUISHMENT_TARGET_TERMS = Set.of(TermType.SECOND);
  private static final Set<TermType> DEVELOPMENT_CONSENT_TARGET_TERMS = Set.of(TermType.THIRD);

  private final LicenceScheduleService licenceScheduleService;
  private final LicenceContinuationService licenceContinuationService;

  public OtherRequirementsVisibilityResolverService(
      LicenceScheduleService licenceScheduleService,
      LicenceContinuationService licenceContinuationService
  ) {
    this.licenceScheduleService = licenceScheduleService;
    this.licenceContinuationService = licenceContinuationService;
  }

  public OtherRequirementsVisibility resolveVisibility(LicenceContinuationApplicationDetail applicationDetail) {
    var scheduleDetail = licenceContinuationService.getScheduleDetailFromApplicationDetail(applicationDetail);
    return resolveVisibility(scheduleDetail);
  }

  public OtherRequirementsVisibility resolveVisibility(LicenceScheduleDetail scheduleDetail) {
    var scheduleState = licenceScheduleService.getScheduleState(scheduleDetail);

    var phaseType = scheduleState.nextPhase() != null ? scheduleState.nextPhase().getPhaseType() : null;
    var termType = scheduleState.nextTerm() != null ? scheduleState.nextTerm().getTermType() : null;

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
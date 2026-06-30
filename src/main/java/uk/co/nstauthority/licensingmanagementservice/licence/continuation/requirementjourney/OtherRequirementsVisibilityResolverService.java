package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.ScheduleState;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;

@Service
public class OtherRequirementsVisibilityResolverService {

  private static final Set<PhaseType> FINANCIAL_CAPACITY_TARGET_PHASES = Set.of(PhaseType.PHASE_C);
  private static final Set<TermType> FINANCIAL_CAPACITY_TARGET_TERMS = Set.of(TermType.SECOND, TermType.THIRD);
  private static final Set<TermType> DEVELOPMENT_CONSENT_TARGET_TERMS = Set.of(TermType.THIRD);

  private final LicenceContinuationService licenceContinuationService;
  private final OtherScheduleEventService otherScheduleEventService;

  public OtherRequirementsVisibilityResolverService(
      LicenceContinuationService licenceContinuationService,
      OtherScheduleEventService otherScheduleEventService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.otherScheduleEventService = otherScheduleEventService;
  }

  public OtherRequirementsVisibility resolveVisibility(LicenceContinuationApplicationDetail applicationDetail) {
    var scheduleDetail = licenceContinuationService.getScheduleDetailFromApplicationDetail(applicationDetail);
    var scheduleState = licenceContinuationService.resolveScheduleState(applicationDetail);

    return resolveVisibility(scheduleDetail, scheduleState);
  }

  private OtherRequirementsVisibility resolveVisibility(LicenceScheduleDetail scheduleDetail, ScheduleState scheduleState) {
    var phaseType = scheduleState.nextPhase() != null ? scheduleState.nextPhase().getPhaseType() : null;
    var termType = scheduleState.nextTerm() != null ? scheduleState.nextTerm().getTermType() : null;

    var showFinancial = (phaseType != null && FINANCIAL_CAPACITY_TARGET_PHASES.contains(phaseType))
                        || (termType != null && FINANCIAL_CAPACITY_TARGET_TERMS.contains(termType));

    var showRelinquishment = otherScheduleEventService.hasEventWithinScheduleWindow(
        scheduleDetail,
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT,
        scheduleState
    );

    var showDevelopmentConsent = termType != null && DEVELOPMENT_CONSENT_TARGET_TERMS.contains(termType);

    return new OtherRequirementsVisibility(
        showFinancial,
        showRelinquishment,
        showDevelopmentConsent
    );
  }
}
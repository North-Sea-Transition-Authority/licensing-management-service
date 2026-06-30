package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.ScheduleState;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;

@ExtendWith(MockitoExtension.class)
class OtherRequirementsVisibilityResolverServiceTest {

  @Mock
  private LicenceContinuationService licenceContinuationService;

  @Mock
  private OtherScheduleEventService otherScheduleEventService;

  @Mock
  private LicenceSchedulePhase licenceSchedulePhase;

  @Mock
  private LicenceScheduleTerm licenceScheduleTerm;

  @InjectMocks
  private OtherRequirementsVisibilityResolverService resolverService;

  private LicenceContinuationApplicationDetail applicationDetail;
  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    applicationDetail = new LicenceContinuationApplicationDetail();
    licenceScheduleDetail = new LicenceScheduleDetail();

    when(licenceContinuationService.getScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(licenceScheduleDetail);
  }

  @Test
  void resolveVisibility_WhenNextIsPhaseC_ShowsOnlyFinancialCapacity() {
    when(licenceSchedulePhase.getPhaseType()).thenReturn(PhaseType.PHASE_C);

    var state = new ScheduleState(licenceScheduleTerm, null, null, licenceSchedulePhase);
    when(licenceContinuationService.resolveScheduleState(applicationDetail)).thenReturn(state);
    when(otherScheduleEventService.hasEventWithinScheduleWindow(licenceScheduleDetail, OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT, state)).thenReturn(false);

    var result = resolverService.resolveVisibility(applicationDetail);

    assertThat(result.showFinancialCapacity()).isTrue();
    assertThat(result.showRelinquishment()).isFalse();
    assertThat(result.showDevelopmentConsent()).isFalse();
    assertThat(result.hasAnyRequirements()).isTrue();
  }

  @Test
  void resolveVisibility_WhenNextIsPhaseB_ShowsNoRequirements() {
    when(licenceSchedulePhase.getPhaseType()).thenReturn(PhaseType.PHASE_B);

    var state = new ScheduleState(licenceScheduleTerm, null, null, licenceSchedulePhase);
    when(licenceContinuationService.resolveScheduleState(applicationDetail)).thenReturn(state);
    when(otherScheduleEventService.hasEventWithinScheduleWindow(licenceScheduleDetail, OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT, state)).thenReturn(false);

    var result = resolverService.resolveVisibility(applicationDetail);

    assertThat(result.showFinancialCapacity()).isFalse();
    assertThat(result.showRelinquishment()).isFalse();
    assertThat(result.showDevelopmentConsent()).isFalse();
    assertThat(result.hasAnyRequirements()).isFalse();
  }

  @Test
  void resolveVisibility_WhenRelinquishmentEventInWindow_ShowsRelinquishment() {
    when(licenceScheduleTerm.getTermType()).thenReturn(TermType.SECOND);

    var state = new ScheduleState(null, null, licenceScheduleTerm, null);
    when(licenceContinuationService.resolveScheduleState(applicationDetail)).thenReturn(state);
    when(otherScheduleEventService.hasEventWithinScheduleWindow(licenceScheduleDetail, OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT, state)).thenReturn(true);

    var result = resolverService.resolveVisibility(applicationDetail);

    assertThat(result.showFinancialCapacity()).isTrue();
    assertThat(result.showRelinquishment()).isTrue();
    assertThat(result.showDevelopmentConsent()).isFalse();
    assertThat(result.hasAnyRequirements()).isTrue();
  }

  @Test
  void resolveVisibility_WhenNoRelinquishmentEventInWindow_HidesRelinquishment() {
    when(licenceScheduleTerm.getTermType()).thenReturn(TermType.SECOND);

    var state = new ScheduleState(null, null, licenceScheduleTerm, null);
    when(licenceContinuationService.resolveScheduleState(applicationDetail)).thenReturn(state);
    when(otherScheduleEventService.hasEventWithinScheduleWindow(licenceScheduleDetail, OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT, state)).thenReturn(false);

    var result = resolverService.resolveVisibility(applicationDetail);

    assertThat(result.showFinancialCapacity()).isTrue();
    assertThat(result.showRelinquishment()).isFalse();
    assertThat(result.showDevelopmentConsent()).isFalse();
    assertThat(result.hasAnyRequirements()).isTrue();
  }

  @Test
  void resolveVisibility_WhenNextTermIsThirdTerm_ShowsFinancialAndFieldDetermination() {
    when(licenceScheduleTerm.getTermType()).thenReturn(TermType.THIRD);

    var state = new ScheduleState(null, null, licenceScheduleTerm, null);
    when(licenceContinuationService.resolveScheduleState(applicationDetail)).thenReturn(state);
    when(otherScheduleEventService.hasEventWithinScheduleWindow(licenceScheduleDetail, OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT, state)).thenReturn(false);

    var result = resolverService.resolveVisibility(applicationDetail);

    assertThat(result.showFinancialCapacity()).isTrue();
    assertThat(result.showRelinquishment()).isFalse();
    assertThat(result.showDevelopmentConsent()).isTrue();
    assertThat(result.hasAnyRequirements()).isTrue();
  }

  @Test
  void resolveVisibility_WhenNoNextPhaseOrTerm_ShowsNoRequirements() {
    var state = new ScheduleState(null, null, null, null);
    when(licenceContinuationService.resolveScheduleState(applicationDetail)).thenReturn(state);
    when(otherScheduleEventService.hasEventWithinScheduleWindow(licenceScheduleDetail, OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT, state)).thenReturn(false);

    var result = resolverService.resolveVisibility(applicationDetail);

    assertThat(result.showFinancialCapacity()).isFalse();
    assertThat(result.showRelinquishment()).isFalse();
    assertThat(result.showDevelopmentConsent()).isFalse();
    assertThat(result.hasAnyRequirements()).isFalse();
  }

}

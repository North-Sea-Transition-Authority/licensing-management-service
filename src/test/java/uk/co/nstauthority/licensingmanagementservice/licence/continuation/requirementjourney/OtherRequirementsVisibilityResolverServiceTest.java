package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@ExtendWith(MockitoExtension.class)
class OtherRequirementsVisibilityResolverServiceTest {

  @Mock
  private LicenceScheduleService licenceScheduleService;
  
  @Mock
  private LicenceSchedulePhase licenceSchedulePhase;
  
  @Mock
  private LicenceScheduleTerm licenceScheduleTerm;

  @InjectMocks
  private OtherRequirementsVisibilityResolverService resolverService;

  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    licenceScheduleDetail = new LicenceScheduleDetail();
  }

  @Test
  void resolve_WhenNextIsPhaseC_ShowsOnlyFinancialCapacity() {
    when(licenceSchedulePhase.getPhaseType()).thenReturn(PhaseType.PHASE_C);
    when(licenceScheduleTerm.getTermType()).thenReturn(TermType.INITIAL);
    when(licenceScheduleService.getNextPhase(licenceScheduleDetail)).thenReturn(Optional.of(licenceSchedulePhase));
    when(licenceScheduleService.getCurrentTerm(licenceScheduleDetail)).thenReturn(licenceScheduleTerm);

    var result = resolverService.resolve(licenceScheduleDetail);

    assertThat(result.showFinancialCapacity()).isTrue();
    assertThat(result.showRelinquishment()).isFalse();
    assertThat(result.showDevelopmentConsent()).isFalse();
    assertThat(result.hasAnyRequirements()).isTrue();
  }

  @Test
  void resolve_WhenNextIsPhaseB_ShowsNoRequirements() {
    when(licenceSchedulePhase.getPhaseType()).thenReturn(PhaseType.PHASE_B);
    when(licenceScheduleTerm.getTermType()).thenReturn(TermType.INITIAL);
    when(licenceScheduleService.getNextPhase(licenceScheduleDetail)).thenReturn(Optional.of(licenceSchedulePhase));
    when(licenceScheduleService.getCurrentTerm(licenceScheduleDetail)).thenReturn(licenceScheduleTerm);

    var result = resolverService.resolve(licenceScheduleDetail);

    assertThat(result.showFinancialCapacity()).isFalse();
    assertThat(result.showRelinquishment()).isFalse();
    assertThat(result.showDevelopmentConsent()).isFalse();
    assertThat(result.hasAnyRequirements()).isFalse();
  }

  @Test
  void resolve_WhenNextIsSecondTerm_ShowsFinancialAndRelinquishment() {
    when(licenceScheduleTerm.getTermType()).thenReturn(TermType.SECOND);
    when(licenceScheduleService.getNextPhase(licenceScheduleDetail))
        .thenReturn(Optional.empty());
    when(licenceScheduleService.getNextTerm(licenceScheduleDetail))
        .thenReturn(Optional.of(licenceScheduleTerm));

    var result = resolverService.resolve(licenceScheduleDetail);

    assertThat(result.showFinancialCapacity()).isTrue();
    assertThat(result.showRelinquishment()).isTrue();
    assertThat(result.showDevelopmentConsent()).isFalse();
    assertThat(result.hasAnyRequirements()).isTrue();
  }

  @Test
  void resolve_WhenNextTermIsThirdTerm_ShowsFinancialAndFieldDetermination() {
    when(licenceScheduleTerm.getTermType()).thenReturn(TermType.THIRD);
    when(licenceScheduleService.getNextPhase(licenceScheduleDetail)).thenReturn(Optional.empty());
    when(licenceScheduleService.getNextTerm(licenceScheduleDetail)).thenReturn(Optional.of(licenceScheduleTerm));

    var result = resolverService.resolve(licenceScheduleDetail);

    assertThat(result.showFinancialCapacity()).isTrue();
    assertThat(result.showRelinquishment()).isFalse();
    assertThat(result.showDevelopmentConsent()).isTrue();
    assertThat(result.hasAnyRequirements()).isTrue();
  }

  @Test
  void resolve_WhenNoNextPhaseOrTerm_ShowsNoRequirements() {
    when(licenceScheduleService.getNextPhase(licenceScheduleDetail)).thenReturn(Optional.empty());
    when(licenceScheduleService.getNextTerm(licenceScheduleDetail)).thenReturn(Optional.empty());

    var result = resolverService.resolve(licenceScheduleDetail);

    assertThat(result.showFinancialCapacity()).isFalse();
    assertThat(result.showRelinquishment()).isFalse();
    assertThat(result.showDevelopmentConsent()).isFalse();
    assertThat(result.hasAnyRequirements()).isFalse();
  }
}
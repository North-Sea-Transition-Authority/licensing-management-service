package uk.co.nstauthority.licensingmanagementservice.licence.schedule.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleRelativeOptionsServiceTest {

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @InjectMocks
  private LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;

  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    var licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
  }

  @Test
  void getScheduleTermOptions() {
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setTermType(TermType.INITIAL);

    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setTermType(TermType.SECOND);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));

    var expectedResult = Map.of(
        term.getId().toString(), term.getTermType().getDisplayName(),
        term2.getId().toString(), term2.getTermType().getDisplayName()
    );

    assertThat(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).isEqualTo(expectedResult);
  }

  @Test
  void getSchedulePhaseOptions() {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setPhaseType(PhaseType.PHASE_A);

    var phase2 = new LicenceSchedulePhase();
    phase2.setId(UUID.randomUUID());
    phase2.setPhaseType(PhaseType.PHASE_B);

    when(licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(phase, phase2));

    var expectedResult = Map.of(
        phase.getId().toString(), phase.getPhaseType().getDisplayName(),
        phase2.getId().toString(), phase2.getPhaseType().getDisplayName()
    );

    assertThat(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).isEqualTo(expectedResult);
  }

  @Test
  void getRelativeEventOptions() {
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setTermType(TermType.INITIAL);

    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setTermType(TermType.SECOND);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));

    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setPhaseType(PhaseType.PHASE_A);

    var phase2 = new LicenceSchedulePhase();
    phase2.setId(UUID.randomUUID());
    phase2.setPhaseType(PhaseType.PHASE_B);

    when(licenceSchedulePhaseService.getActivePhasesByTerm(term)).thenReturn(List.of(phase, phase2));

    var expectedResult = Map.of(
        phase.getId().toString(), "Start of %s".formatted(phase.getPhaseType().getDisplayName()),
        phase2.getId().toString(), "Start of %s".formatted(phase2.getPhaseType().getDisplayName()),
        term2.getId().toString(), "Start of %s".formatted(term2.getTermType().getDisplayName())
    );

    assertThat(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).isEqualTo(expectedResult);
  }

}
package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSchedulePhaseTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleServiceTest {

  @Mock
  private LicenceScheduleRepository licenceScheduleRepository;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

  @Mock
  private Clock clock;

  @InjectMocks
  private LicenceScheduleService licenceScheduleService;

  @Captor
  private ArgumentCaptor<LicenceSchedule> licenceScheduleArgumentCaptor;

  @Test
  void getOrCreateNewLicenceScheduleForLicence() {
    var licence = new Licence();

    licenceScheduleService.getOrCreateNewLicenceScheduleForLicence(licence);

    verify(licenceScheduleRepository).save(licenceScheduleArgumentCaptor.capture());

    assertEquals(licence, licenceScheduleArgumentCaptor.getValue().getLicence());
  }

  @Test
  void getNextTermPhaseStartDate_findsNextPhaseInSameTerm() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    var currentTerm = LicenceScheduleTermTestUtil.builder()
        .withStartDate(LocalDate.of(2025, 1, 1))
        .withEndDate(LocalDate.of(2030, 1, 1))
        .build();
    var currentPhase = LicenceSchedulePhaseTestUtil.builder()
        .withStartDate(LocalDate.of(2025, 1, 1))
        .withEndDate(LocalDate.of(2026, 1, 1))
        .build();
    var nextPhase = LicenceSchedulePhaseTestUtil.builder()
        .withStartDate(LocalDate.of(2026, 1, 1))
        .withEndDate(LocalDate.of(2027, 1, 1))
        .build();

    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(scheduleDetail)).thenReturn(List.of(currentTerm));
    when(licenceTypeRulesResolver.hasPhases(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);
    when(licenceSchedulePhaseService.getActivePhasesByTerm(currentTerm)).thenReturn(List.of(currentPhase, nextPhase));

    var result = licenceScheduleService.getNextTermPhaseStartDate(scheduleDetail);

    assertThat(result).hasValue(LocalDate.of(2026, 1, 1));
  }

  @Test
  void getNextTermPhaseStartDate_findsNextTermStartIfNoNextPhase() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    var currentTerm = LicenceScheduleTermTestUtil.builder()
        .withStartDate(LocalDate.of(2025, 1, 1))
        .withEndDate(LocalDate.of(2030, 1, 1))
        .build();
    var currentPhase = LicenceSchedulePhaseTestUtil.builder()
        .withStartDate(LocalDate.of(2025, 1, 1))
        .withEndDate(LocalDate.of(2030, 1, 1))
        .build();
    var nextTerm = LicenceScheduleTermTestUtil.builder()
        .withStartDate(LocalDate.of(2030, 1, 1))
        .withEndDate(LocalDate.of(2035, 1, 1))
        .build();

    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(scheduleDetail)).thenReturn(List.of(currentTerm, nextTerm));
    when(licenceTypeRulesResolver.hasPhases(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);
    when(licenceSchedulePhaseService.getActivePhasesByTerm(currentTerm)).thenReturn(List.of(currentPhase));
    when(licenceSchedulePhaseService.getActivePhasesByTerm(nextTerm)).thenReturn(List.of());

    var result = licenceScheduleService.getNextTermPhaseStartDate(scheduleDetail);

    assertThat(result).hasValue(LocalDate.of(2030, 1, 1));
  }

  @Test
  void getNextTermPhaseStartDate_returnsEmptyIfNoCurrentTerm() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(scheduleDetail)).thenReturn(List.of());

    var result = licenceScheduleService.getNextTermPhaseStartDate(scheduleDetail);

    assertThat(result).isEmpty();
  }

  @Test
  void getScheduleState_WhenNoCurrentTerm_ReturnsAllNulls() {
    var detail = new LicenceScheduleDetail();

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(detail))
        .thenReturn(Collections.emptyList());

    var state = licenceScheduleService.getScheduleState(detail);

    assertThat(state.currentTerm()).isNull();
    assertThat(state.currentPhase()).isNull();
    assertThat(state.nextTerm()).isNull();
    assertThat(state.nextPhase()).isNull();
  }

  @Test
  void getScheduleState_WhenHasNextTermAndNextPhase_ReturnsBoth() {
    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var detail = new LicenceScheduleDetail();

    var currentTerm = new LicenceScheduleTerm();
    currentTerm.setStartDate(LocalDate.of(2025, 1, 1));
    currentTerm.setEndDate(LocalDate.of(2030, 1, 1));

    var nextTerm = new LicenceScheduleTerm();
    nextTerm.setStartDate(LocalDate.of(2030, 1, 1));
    nextTerm.setEndDate(LocalDate.of(2035, 1, 1));

    var currentPhase = new LicenceSchedulePhase();
    currentPhase.setStartDate(LocalDate.of(2025, 1, 1));
    currentPhase.setEndDate(LocalDate.of(2026, 1, 1));

    var nextPhase = new LicenceSchedulePhase();
    nextPhase.setStartDate(LocalDate.of(2026, 1, 1));
    nextPhase.setEndDate(LocalDate.of(2027, 1, 1));

    var terms = List.of(currentTerm, nextTerm);
    var phases = List.of(currentPhase, nextPhase);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(detail)).thenReturn(terms);
    when(licenceSchedulePhaseService.getActivePhasesByTerm(currentTerm)).thenReturn(phases);

    var state = licenceScheduleService.getScheduleState(detail);

    assertThat(state.currentTerm()).isEqualTo(currentTerm);
    assertThat(state.currentPhase()).isEqualTo(currentPhase);
    assertThat(state.nextTerm()).isEqualTo(nextTerm);
    assertThat(state.nextPhase()).isEqualTo(nextPhase);
  }

  @Test
  void getScheduleState_WhenNoNextPhase_FindsNextTerm() {
    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var detail = new LicenceScheduleDetail();

    var currentTerm = new LicenceScheduleTerm();
    currentTerm.setStartDate(LocalDate.of(2025, 1, 1));
    currentTerm.setEndDate(LocalDate.of(2030, 1, 1));

    var nextTerm = new LicenceScheduleTerm();
    nextTerm.setStartDate(LocalDate.of(2030, 1, 1));
    nextTerm.setEndDate(LocalDate.of(2035, 1, 1));

    var currentPhase = new LicenceSchedulePhase();
    currentPhase.setStartDate(LocalDate.of(2025, 1, 1));
    currentPhase.setEndDate(LocalDate.of(2030, 1, 1));

    var terms = List.of(currentTerm, nextTerm);
    var phases = List.of(currentPhase);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(detail)).thenReturn(terms);
    when(licenceSchedulePhaseService.getActivePhasesByTerm(currentTerm)).thenReturn(phases);

    var state = licenceScheduleService.getScheduleState(detail);

    assertThat(state.currentTerm()).isEqualTo(currentTerm);
    assertThat(state.currentPhase()).isEqualTo(currentPhase);
    assertThat(state.nextTerm()).isEqualTo(nextTerm);
    assertThat(state.nextPhase()).isNull();
  }

  @Test
  void getScheduleState_WhenNoPhasesInCurrentTerm_FindsNextTerm() {
    when(clock.instant()).thenReturn(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var detail = new LicenceScheduleDetail();

    var currentTerm = new LicenceScheduleTerm();
    currentTerm.setStartDate(LocalDate.of(2025, 1, 1));
    currentTerm.setEndDate(LocalDate.of(2030, 1, 1));

    var nextTerm = new LicenceScheduleTerm();
    nextTerm.setStartDate(LocalDate.of(2030, 1, 1));
    nextTerm.setEndDate(LocalDate.of(2035, 1, 1));

    var terms = List.of(currentTerm, nextTerm);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(detail)).thenReturn(terms);
    when(licenceSchedulePhaseService.getActivePhasesByTerm(currentTerm)).thenReturn(Collections.emptyList());

    var state = licenceScheduleService.getScheduleState(detail);

    assertThat(state.currentTerm()).isEqualTo(currentTerm);
    assertThat(state.currentPhase()).isNull();
    assertThat(state.nextTerm()).isEqualTo(nextTerm);
    assertThat(state.nextPhase()).isNull();
  }
}
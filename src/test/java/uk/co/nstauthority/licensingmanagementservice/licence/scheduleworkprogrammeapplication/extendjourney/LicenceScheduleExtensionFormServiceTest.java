package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleExtensionFormServiceTest {

  @Mock
  Clock clock;

  @Mock
  private LicenceScheduleExtensionRepository licenceScheduleExtensionRepository;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceScheduleExtensionService licenceScheduleExtensionService;

  @InjectMocks
  private LicenceScheduleExtensionFormService licenceScheduleExtensionFormService;

  @Captor
  private ArgumentCaptor<LicenceScheduleExtensionRequest> licenceScheduleExtensionRequestArgumentCaptor;

  @Test
  void saveExtensionForm() {
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var form = new LicenceScheduleExtensionForm();
    form.setExplanation("testExplanation");
    form.getExtensionDuration().setYears("4");
    form.getExtensionDuration().setMonths("4");
    form.getExtensionDuration().setDays("4");

    licenceScheduleExtensionFormService.saveExtensionForm(form, scheduleWorkProgrammeApplicationDetail);

    verify(licenceScheduleExtensionRepository).save(licenceScheduleExtensionRequestArgumentCaptor.capture());

    var result = licenceScheduleExtensionRequestArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceScheduleExtensionRequest::getExtensionDuration,
        LicenceScheduleExtensionRequest::getExplanation,
        LicenceScheduleExtensionRequest::getScheduleWorkProgrammeApplicationDetails
    ).containsExactly(
        form.getExtensionDuration().toThreeFieldDuration(),
        form.getExplanation(),
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @Test
  void getCurrentTerm() {
    clockSetup();

    var licenceScheduleDetail = new LicenceScheduleDetail();
    var currentTerm = new LicenceScheduleTerm();
    currentTerm.setLicenceScheduleDetail(licenceScheduleDetail);
    currentTerm.setStartDate(LocalDate.now(clock).minusDays(1));
    currentTerm.setEndDate(LocalDate.now(clock).plusYears(1));

    var notCurrent = new LicenceScheduleTerm();
    notCurrent.setLicenceScheduleDetail(licenceScheduleDetail);
    notCurrent.setStartDate(LocalDate.now(clock).plusYears(1));
    notCurrent.setEndDate(LocalDate.now(clock).plusYears(3));

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(currentTerm, notCurrent));

    assertThat(licenceScheduleExtensionFormService.getCurrentTerm(licenceScheduleDetail)).isEqualTo(currentTerm);
  }

  @Test
  void getCurrentPhase() {
    clockSetup();

    var licenceScheduleDetail = new LicenceScheduleDetail();
    var currentPhase = new LicenceSchedulePhase();
    currentPhase.setLicenceScheduleDetail(licenceScheduleDetail);
    currentPhase.setStartDate(LocalDate.now(clock).minusDays(1));
    currentPhase.setEndDate(LocalDate.now(clock).plusYears(1));

    var laterPhase = new LicenceSchedulePhase();
    laterPhase.setLicenceScheduleDetail(licenceScheduleDetail);
    laterPhase.setStartDate(LocalDate.now(clock).plusYears(1));
    laterPhase.setEndDate(LocalDate.now(clock).plusYears(3));

    when(licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(any()))
        .thenReturn(List.of(currentPhase, laterPhase));

    assertThat(licenceScheduleExtensionFormService.getCurrentPhase(licenceScheduleDetail)).isEqualTo(currentPhase);
  }

  @Test
  void isCurrentlyActive() {
    clockSetup();

    LocalDate now = LocalDate.now(clock);
    LocalDate startDate = now.minusDays(1);
    LocalDate endDate = now.plusYears(1);
    assertThat(licenceScheduleExtensionFormService.isCurrentlyActive(startDate, endDate)).isTrue();
  }

  @Test
  void isNotCurrentlyActive() {
    clockSetup();

    LocalDate now = LocalDate.now(clock);
    LocalDate startDate = now.plusYears(5);
    LocalDate endDate = now.plusYears(10);
    assertThat(licenceScheduleExtensionFormService.isCurrentlyActive(startDate, endDate)).isFalse();
  }

  @Test
  void getLicenceScheduleExtensionExisting() {
    ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    LicenceScheduleExtensionRequest licenceScheduleExtensionRequest = new LicenceScheduleExtensionRequest();
    licenceScheduleExtensionRequest.setExtensionDuration(new ThreeFieldDuration(1, 1, 1));
    licenceScheduleExtensionRequest.setExplanation("testExplanation");
    licenceScheduleExtensionRequest.setId(UUID.randomUUID());
    licenceScheduleExtensionRequest.setScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
    when(licenceScheduleExtensionService.getExtensionRequestByScheduleWorkProgrammeApplicationDetail(
        any())).thenReturn(Optional.of(licenceScheduleExtensionRequest));

    LicenceScheduleExtensionForm actualForm = licenceScheduleExtensionFormService.getLicenceScheduleExtensionForm(
        scheduleWorkProgrammeApplicationDetail);

    assertEquals(actualForm.getExtensionDuration().toThreeFieldDuration(),
        licenceScheduleExtensionRequest.getExtensionDuration());
    assertEquals(("testExplanation"), actualForm.getExplanation());
  }

  private void clockSetup() {
    when(clock.instant()).thenReturn(Instant.parse("2025-09-09T00:00:00.00Z"));
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
  }
}
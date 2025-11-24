package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSchedulePhaseTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleExtensionServiceTest {

  @Mock
  private LicenceScheduleExtensionRepository licenceScheduleExtensionRepository;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  @Mock
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @Mock
  private Clock clock;

  @InjectMocks
  private LicenceScheduleExtensionService licenceScheduleExtensionService;

  @Captor
  private ArgumentCaptor<LicenceScheduleExtensionRequest> licenceScheduleExtensionRequestArgumentCaptor;

  private static final LocalDate TODAY = LocalDate.parse("2025-09-09");
  private static final LocalDate DATE_FUTURE = TODAY.plusYears(2);
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @BeforeEach
  void setUp() {
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(new ScheduleWorkProgrammeApplication());
    scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication().setLicenceScheduleDetail(new LicenceScheduleDetail());
  }

  @Test
  void saveExtensionForm_createsTermAndPhaseRequestsWhenBothSelected() {
    var form = new LicenceScheduleExtensionForm();
    var termId = UUID.randomUUID();
    var phaseId = UUID.randomUUID();

    LicenceScheduleTerm licenceScheduleTerm = LicenceScheduleTermTestUtil.builder().withId(termId).build();
    LicenceSchedulePhase licenceSchedulePhase = LicenceSchedulePhaseTestUtil.builder().withId(phaseId).build();

    form.getExtensionDuration().put(termId.toString(), createDurationInput("1", "0", "0"));
    form.getExtensionDuration().put(phaseId.toString(), createDurationInput("0", "3", "0"));
    form.setSelectedTerm(Map.of(termId.toString(), true));
    form.setSelectedPhase(Map.of(phaseId.toString(), true));

    when(licenceScheduleExtensionRepository.findByScheduleWorkProgrammeApplicationDetailsAndLicenceScheduleTermId(scheduleWorkProgrammeApplicationDetail, termId)).thenReturn(Optional.empty());
    when(licenceScheduleExtensionRepository.findByScheduleWorkProgrammeApplicationDetailsAndLicenceSchedulePhaseId(scheduleWorkProgrammeApplicationDetail, phaseId)).thenReturn(Optional.empty());
    when(licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail)).thenReturn(Collections.emptyList());
    when(licenceScheduleTermRepository.findById(termId)).thenReturn(Optional.of(licenceScheduleTerm));
    when(licenceSchedulePhaseRepository.findById(phaseId)).thenReturn(Optional.of(licenceSchedulePhase));

    licenceScheduleExtensionService.saveExtensionForm(form, scheduleWorkProgrammeApplicationDetail);

    verify(licenceScheduleExtensionRepository, times(2)).save(licenceScheduleExtensionRequestArgumentCaptor.capture());

    List<LicenceScheduleExtensionRequest> savedRequests = licenceScheduleExtensionRequestArgumentCaptor.getAllValues();

    var savedTermRequest = savedRequests.stream().filter(req -> req.getLicenceScheduleTerm() != null).findFirst().orElseThrow();
    var savedPhaseRequest = savedRequests.stream().filter(req -> req.getLicenceSchedulePhase() != null).findFirst().orElseThrow();

    assertThat(savedTermRequest.getExtensionDuration().years()).isEqualTo(1);
    assertThat(savedTermRequest.getLicenceScheduleTerm().getId()).isEqualTo(termId);

    assertThat(savedPhaseRequest.getExtensionDuration().months()).isEqualTo(3);
    assertThat(savedPhaseRequest.getLicenceSchedulePhase().getId()).isEqualTo(phaseId);
  }
  @Test
  void saveExtensionForm_updatesExistingTermRequest() {
    var form = new LicenceScheduleExtensionForm();
    var termId = UUID.randomUUID();

    form.setSelectedTerm(Map.of(termId.toString(), true));
    form.getExtensionDuration().put(termId.toString(), createDurationInput("2", "0", "0"));

    var existingRequest = new LicenceScheduleExtensionRequest();
    existingRequest.setLicenceScheduleTerm(new LicenceScheduleTerm());
    existingRequest.getLicenceScheduleTerm().setId(termId);

    when(licenceScheduleExtensionRepository.findByScheduleWorkProgrammeApplicationDetailsAndLicenceScheduleTermId(any(), eq(termId))).thenReturn(Optional.of(existingRequest));
    when(licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(any())).thenReturn(Collections.emptyList());
    when(licenceScheduleTermRepository.findById(any())).thenReturn(Optional.of(existingRequest.getLicenceScheduleTerm()));

    licenceScheduleExtensionService.saveExtensionForm(form, scheduleWorkProgrammeApplicationDetail);

    verify(licenceScheduleExtensionRepository).save(licenceScheduleExtensionRequestArgumentCaptor.capture());
    assertThat(licenceScheduleExtensionRequestArgumentCaptor.getValue().getExtensionDuration().years()).isEqualTo(2);
  }

  @Test
  void saveExtensionForm_handlesSingleTermRequestFallback() {
    var form = new LicenceScheduleExtensionForm();
    var termId = UUID.randomUUID();

    form.getExtensionDuration().put(termId.toString(), createDurationInput("1", "0", "0"));

    LicenceScheduleTerm licenceScheduleTerm = LicenceScheduleTermTestUtil.builder().withId(termId).build();

    when(licenceScheduleTermRepository.existsById(termId)).thenReturn(true);
    when(licenceSchedulePhaseRepository.existsById(termId)).thenReturn(false);
    when(licenceScheduleExtensionRepository.findByScheduleWorkProgrammeApplicationDetailsAndLicenceScheduleTermId(any(), eq(termId))).thenReturn(Optional.empty());
    when(licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(any())).thenReturn(Collections.emptyList());

    when(licenceScheduleTermRepository.findById(termId)).thenReturn(Optional.of(licenceScheduleTerm));

    licenceScheduleExtensionService.saveExtensionForm(form, scheduleWorkProgrammeApplicationDetail);

    verify(licenceScheduleExtensionRepository).save(licenceScheduleExtensionRequestArgumentCaptor.capture());

    assertThat(licenceScheduleExtensionRequestArgumentCaptor.getValue().getLicenceScheduleTerm().getId()).isEqualTo(termId);
  }

  @Test
  void saveExtensionForm_deletesUnselectedRequests() {
    var form = new LicenceScheduleExtensionForm();
    var selectedTermId = UUID.randomUUID();
    var unselectedPhaseId = UUID.randomUUID();

    LicenceScheduleTerm licenceScheduleTerm = LicenceScheduleTermTestUtil.builder().withId(selectedTermId).build();
    LicenceSchedulePhase licenceSchedulePhase = LicenceSchedulePhaseTestUtil.builder().withId(unselectedPhaseId).build();

    form.setSelectedTerm(Map.of(selectedTermId.toString(), true));
    form.getExtensionDuration().put(selectedTermId.toString(), createDurationInput("1", "0", "0"));

    var existingRequestToKeep = new LicenceScheduleExtensionRequest();
    existingRequestToKeep.setLicenceScheduleTerm(licenceScheduleTerm);

    var existingRequestToDelete = new LicenceScheduleExtensionRequest();
    existingRequestToDelete.setLicenceSchedulePhase(licenceSchedulePhase);

    when(licenceScheduleTermRepository.findById(selectedTermId)).thenReturn(Optional.of(licenceScheduleTerm));
    when(licenceScheduleExtensionRepository.findByScheduleWorkProgrammeApplicationDetailsAndLicenceScheduleTermId(any(), eq(selectedTermId))).thenReturn(Optional.of(existingRequestToKeep));
    when(licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(any())).thenReturn(List.of(existingRequestToKeep, existingRequestToDelete));

    licenceScheduleExtensionService.saveExtensionForm(form, scheduleWorkProgrammeApplicationDetail);

    verify(licenceScheduleExtensionRepository).delete(existingRequestToDelete);
    verify(licenceScheduleExtensionRepository).save(any(LicenceScheduleExtensionRequest.class));
  }

  @Test
  void getLicenceScheduleExtensionFormBuildsFormFromExistingRequests() {
    mockClock();

    var termId = UUID.randomUUID();
    var phaseId = UUID.randomUUID();

    LicenceScheduleTerm licenceScheduleTerm = LicenceScheduleTermTestUtil.builder().withId(termId).build();
    LicenceSchedulePhase licenceSchedulePhase = LicenceSchedulePhaseTestUtil.builder().withId(phaseId).build();

    var termRequest = new LicenceScheduleExtensionRequest();
    termRequest.setLicenceScheduleTerm(licenceScheduleTerm);
    termRequest.setExtensionDuration(new ThreeFieldDuration(2, 0, 0));

    var phaseRequest = new LicenceScheduleExtensionRequest();
    phaseRequest.setLicenceSchedulePhase(licenceSchedulePhase);
    phaseRequest.setExtensionDuration(new ThreeFieldDuration(0, 3, 0));

    when(licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(any())).thenReturn(List.of(termRequest, phaseRequest));

    var form = licenceScheduleExtensionService.getlicenceScheduleExtensionForm(scheduleWorkProgrammeApplicationDetail);

    assertThat(form.getSelectedTerm()).containsEntry(termId.toString(), true);
    assertThat(form.getSelectedPhase()).containsEntry(phaseId.toString(), true);
    assertThat(form.getExtensionDuration().get(termId.toString()).toThreeFieldDuration()).isEqualTo(new ThreeFieldDuration(2, 0, 0));
  }

  @Test
  void getCurrentTerm_findsActiveTerm() {
    mockClock();
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var activeTerm = LicenceScheduleTermTestUtil.builder()
                                                .withStartDate(TODAY.minusDays(5))
                                                .withEndDate(TODAY.plusYears(1))
                                                .build();

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(activeTerm));

    var result = licenceScheduleExtensionService.getCurrentTerm(licenceScheduleDetail);
    assertThat(result).isSameAs(activeTerm);
  }

  @Test
  void getCurrentPhase_findsActivePhase() {
    mockClock();

    var term = LicenceScheduleTermTestUtil.builder()
                                                .withStartDate(TODAY.minusDays(10))
                                                .withEndDate(DATE_FUTURE)
                                                .build();

    var activePhase = LicenceSchedulePhaseTestUtil.builder()
                                                  .withStartDate(TODAY.minusDays(5))
                                                  .withEndDate(TODAY.plusYears(1))
                                                  .build();

    when(licenceSchedulePhaseService.getActivePhasesByTerm(term)).thenReturn(List.of(activePhase));

    var result = licenceScheduleExtensionService.getCurrentPhase(term);
    assertThat(result).isSameAs(activePhase);
  }

  @Test
  void isCurrentlyActive_returnsTrueForActivePeriod() {
    mockClock();
    var startDate = TODAY.minusDays(1);
    var endDate = TODAY.plusDays(1);
    assertThat(licenceScheduleExtensionService.isCurrentlyActive(startDate, endDate)).isTrue();
  }

  @Test
  void canExtendMoreThanOneOption_returnsCorrectCount() {
    var phaseDetail = new LicenceScheduleTermAndPhases.PhaseDetails(UUID.randomUUID().toString(), "Phase A");
    var termWithPhase = new LicenceScheduleTermAndPhases(UUID.randomUUID().toString(), "Term A", List.of(phaseDetail));
    var termOnly = new LicenceScheduleTermAndPhases(UUID.randomUUID().toString(), "Term B", Collections.emptyList());

    var validTermsAndPhases = List.of(termWithPhase, termOnly);

    assertThat(licenceScheduleExtensionService.canExtendMoreThanOneOption(validTermsAndPhases)).isTrue();
    assertThat(licenceScheduleExtensionService.canExtendMoreThanOneOption(null)).isFalse();
  }

  @Test
  void getExtendableRequest_ExtendableTermAndPhases() {
    mockClock();

    var licenceScheduleDetail = new LicenceScheduleDetail();
    var termId = UUID.randomUUID();
    var phaseId = UUID.randomUUID();

    LicenceScheduleTerm term = LicenceScheduleTermTestUtil.builder()
                                                          .withId(termId)
                                                          .withStartDate(TODAY.minusDays(10))
                                                          .withEndDate(DATE_FUTURE)
                                                          .withTermType(TermType.INITIAL)
                                                          .build();

    LicenceSchedulePhase phase = LicenceSchedulePhaseTestUtil.builder()
                                                             .withId(phaseId)
                                                             .withPhaseType(PhaseType.PHASE_A)
                                                             .withStartDate(TODAY.minusDays(5))
                                                             .withEndDate(DATE_FUTURE)
                                                             .build();

    LicenceSchedulePhase pastPhase = LicenceSchedulePhaseTestUtil.builder()
                                                                 .withId(phaseId)
                                                                 .withPhaseType(PhaseType.PHASE_B)
                                                                 .withStartDate(TODAY.minusDays(5))
                                                                 .withEndDate(TODAY.minusDays(5))
                                                                 .build();


    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));

    when(licenceSchedulePhaseRepository.existsByLicenceScheduleTermId(termId)).thenReturn(true);
    when(licenceSchedulePhaseService.getActivePhasesByTerm(term)).thenReturn(List.of(phase, pastPhase));

    List<LicenceScheduleTermAndPhases> result = licenceScheduleExtensionService.getExtendableTermAndPhases(licenceScheduleDetail);

    assertThat(result).hasSize(1);
    LicenceScheduleTermAndPhases resultEntry = result.get(0);
    assertThat(resultEntry.phases()).hasSize(1);
  }

  private void mockClock() {
    when(clock.instant()).thenReturn(Instant.parse("2025-09-09T10:00:00.00Z"));
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
  }

  private ThreeFieldDurationInput createDurationInput(String year, String month, String day) {
    var input = new ThreeFieldDurationInput("test", "test");
    input.setYears(year);
    input.setMonths(month);
    input.setDays(day);
    return input;
  }
}
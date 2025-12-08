package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityFormServiceTest {

  @Mock
  private WorkProgrammeActivityRepository workProgrammeActivityRepository;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

  @InjectMocks
  private WorkProgrammeActivityFormService workProgrammeActivityFormService;

  @Captor
  private ArgumentCaptor<WorkProgrammeActivity> workProgrammeActivityArgumentCaptor;

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
    
    assertThat(workProgrammeActivityFormService.getScheduleTermOptions(licenceScheduleDetail)).isEqualTo(expectedResult);
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

    assertThat(workProgrammeActivityFormService.getSchedulePhaseOptions(licenceScheduleDetail)).isEqualTo(expectedResult);
  }

  @Test
  void getRelativeDateOptions() {
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

    assertThat(workProgrammeActivityFormService.getRelativeDateOptions(licenceScheduleDetail)).isEqualTo(expectedResult);
  }

  @Test
  void getDateOptions() {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setPhaseType(PhaseType.PHASE_A);

    when(licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(phase));

    when(licenceTypeRulesResolver.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);

    assertThat(workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail))
        .isEqualTo(DisplayableEnumOptionUtil.getDisplayableOptions(WorkProgrammeActivityDateOption.class));
  }

  @Test
  void getDateOptions_licenceTypeDoesntHavePhases() {
    var options = new ArrayList<>(Arrays.asList(WorkProgrammeActivityDateOption.values()));
    options.remove(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);

    when(licenceTypeRulesResolver.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION)).thenReturn(false);

    assertThat(workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail))
        .isEqualTo(DisplayableEnumOptionUtil.getDisplayableOptions(options));
  }

  @Test
  void getDateOptions_licenceDoesntHavePhases() {
    var options = new ArrayList<>(Arrays.asList(WorkProgrammeActivityDateOption.values()));
    options.remove(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);

    when(licenceTypeRulesResolver.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);

    assertThat(workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail))
        .isEqualTo(DisplayableEnumOptionUtil.getDisplayableOptions(options));
  }

  @Test
  void saveActivityFromForm_relativeDate_relatedToTerm() {
    var form = new WorkProgrammeActivityForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.WELL_TEST);
    form.setDescription("description");
    form.setWorkProgrammeActivityCommitment(WorkProgrammeActivityCommitment.FIRM);
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);

    var termId = UUID.randomUUID();

    form.setRelativeEventId(String.valueOf(termId));

    var term = new LicenceScheduleTerm();
    term.setId(termId);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));

    var testDuration = new ThreeFieldDuration(1,0,0);

    form.getRelativeDuration().setFromThreeFieldDuration(testDuration);

    workProgrammeActivityFormService.saveActivityFromForm(form, licenceScheduleDetail);

    verify(workProgrammeActivityRepository).save(workProgrammeActivityArgumentCaptor.capture());

    assertThat(workProgrammeActivityArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivity::getLicenceScheduleDetail,
            WorkProgrammeActivity::getCategory,
            WorkProgrammeActivity::getOtherCategoryName,
            WorkProgrammeActivity::getDescription,
            WorkProgrammeActivity::getCommitment,
            WorkProgrammeActivity::getDateOption,
            WorkProgrammeActivity::getDueDate,
            WorkProgrammeActivity::getLicenceScheduleTerm,
            WorkProgrammeActivity::getLicenceSchedulePhase,
            WorkProgrammeActivity::getRelativeDuration
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getWorkProgrammeActivityCategory(),
            null,
            form.getDescription(),
            form.getWorkProgrammeActivityCommitment(),
            form.getWorkProgrammeActivityDateOption(),
            null,
            term,
            null,
            testDuration
        );
  }

  @Test
  void saveActivityFromForm_relativeDate_relatedToPhase() {
    var form = new WorkProgrammeActivityForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.WELL_TEST);
    form.setDescription("description");
    form.setWorkProgrammeActivityCommitment(WorkProgrammeActivityCommitment.FIRM);
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);

    var phaseId = UUID.randomUUID();

    form.setRelativeEventId(String.valueOf(phaseId));

    var phase = new LicenceSchedulePhase();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);
    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of());

    var testDuration = new ThreeFieldDuration(1,0,0);

    form.getRelativeDuration().setFromThreeFieldDuration(testDuration);

    workProgrammeActivityFormService.saveActivityFromForm(form, licenceScheduleDetail);

    verify(workProgrammeActivityRepository).save(workProgrammeActivityArgumentCaptor.capture());

    assertThat(workProgrammeActivityArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivity::getLicenceScheduleDetail,
            WorkProgrammeActivity::getCategory,
            WorkProgrammeActivity::getOtherCategoryName,
            WorkProgrammeActivity::getDescription,
            WorkProgrammeActivity::getCommitment,
            WorkProgrammeActivity::getDateOption,
            WorkProgrammeActivity::getDueDate,
            WorkProgrammeActivity::getLicenceScheduleTerm,
            WorkProgrammeActivity::getLicenceSchedulePhase,
            WorkProgrammeActivity::getRelativeDuration
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getWorkProgrammeActivityCategory(),
            null,
            form.getDescription(),
            form.getWorkProgrammeActivityCommitment(),
            form.getWorkProgrammeActivityDateOption(),
            null,
            null,
            phase,
            testDuration
        );
  }

  @Test
  void saveActivityFromForm_termOption() {
    var form = new WorkProgrammeActivityForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.OTHER_ACTIVITY);
    form.setOtherCategoryName("otherCategoryName");
    form.setDescription("description");
    form.setWorkProgrammeActivityCommitment(WorkProgrammeActivityCommitment.FIRM);
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.WITHIN_A_TERM);

    var termId = UUID.randomUUID();

    form.setLicenceScheduleTermId(String.valueOf(termId));

    var term = new LicenceScheduleTerm();

    when(licenceScheduleTermService.getTermByIdOrThrow(termId)).thenReturn(term);

    workProgrammeActivityFormService.saveActivityFromForm(form, licenceScheduleDetail);

    verify(workProgrammeActivityRepository).save(workProgrammeActivityArgumentCaptor.capture());

    assertThat(workProgrammeActivityArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivity::getLicenceScheduleDetail,
            WorkProgrammeActivity::getCategory,
            WorkProgrammeActivity::getOtherCategoryName,
            WorkProgrammeActivity::getDescription,
            WorkProgrammeActivity::getCommitment,
            WorkProgrammeActivity::getDateOption,
            WorkProgrammeActivity::getDueDate,
            WorkProgrammeActivity::getLicenceScheduleTerm,
            WorkProgrammeActivity::getLicenceSchedulePhase
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getWorkProgrammeActivityCategory(),
            form.getOtherCategoryName(),
            form.getDescription(),
            form.getWorkProgrammeActivityCommitment(),
            form.getWorkProgrammeActivityDateOption(),
            null,
            term,
            null
        );
  }

  @Test
  void saveActivityFromForm_phaseOption() {
    var form = new WorkProgrammeActivityForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.OTHER_ACTIVITY);
    form.setOtherCategoryName("otherCategoryName");
    form.setDescription("description");
    form.setWorkProgrammeActivityCommitment(WorkProgrammeActivityCommitment.FIRM);
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);

    var phaseId = UUID.randomUUID();

    form.setLicenceSchedulePhaseId(String.valueOf(phaseId));

    var phase = new LicenceSchedulePhase();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);

    workProgrammeActivityFormService.saveActivityFromForm(form, licenceScheduleDetail);

    verify(workProgrammeActivityRepository).save(workProgrammeActivityArgumentCaptor.capture());

    assertThat(workProgrammeActivityArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivity::getLicenceScheduleDetail,
            WorkProgrammeActivity::getCategory,
            WorkProgrammeActivity::getOtherCategoryName,
            WorkProgrammeActivity::getDescription,
            WorkProgrammeActivity::getCommitment,
            WorkProgrammeActivity::getDateOption,
            WorkProgrammeActivity::getDueDate,
            WorkProgrammeActivity::getLicenceScheduleTerm,
            WorkProgrammeActivity::getLicenceSchedulePhase
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getWorkProgrammeActivityCategory(),
            form.getOtherCategoryName(),
            form.getDescription(),
            form.getWorkProgrammeActivityCommitment(),
            form.getWorkProgrammeActivityDateOption(),
            null,
            null,
            phase
        );
  }
}
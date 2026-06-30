package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationService;

@ExtendWith(MockitoExtension.class)
class SwpApplicationRequestPurposeServiceTest {

  @Mock
  private LicenceScheduleExtensionRepository licenceScheduleExtensionRepository;

  @Mock
  private LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService;

  @Mock
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

  @Mock
  private SwpApplicationRequestPurposeRepository swpApplicationRequestPurposeRepository;

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Mock
  private LicenceScheduleService licenceScheduleService;

  @InjectMocks
  private SwpApplicationRequestPurposeService underTest;

  @Captor
  private ArgumentCaptor<SwpApplicationRequestPurpose> swpApplicationRequestPurposeArgumentCaptor;

  @Test
  void saveOrUpdateRequestPurpose_NewRequest() {

    var form = new SwpApplicationRequestPurposeForm();
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.empty());

    underTest.saveOrUpdateRequestPurpose(scheduleWorkProgrammeApplicationDetail, form);

    verify(swpApplicationRequestPurposeRepository).save(swpApplicationRequestPurposeArgumentCaptor.capture());

    assertThat(swpApplicationRequestPurposeArgumentCaptor.getValue().getScheduleWorkProgrammeApplicationDetail())
        .isEqualTo(scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void saveOrUpdateRequestPurpose_UpdateRequestPurpose() {

    var form = new SwpApplicationRequestPurposeForm();
    form.setRequestPurposes(Set.of(SwpApplicationRequestPurposeOption.EXTEND_A_TERM));
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose(UUID.randomUUID());
    swpApplicationRequestPurpose.setScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail);

    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(swpApplicationRequestPurpose));

    underTest.saveOrUpdateRequestPurpose(scheduleWorkProgrammeApplicationDetail, form);

    verify(swpApplicationRequestPurposeRepository).save(swpApplicationRequestPurposeArgumentCaptor.capture());

    assertThat(swpApplicationRequestPurposeArgumentCaptor.getValue())
        .extracting(
            SwpApplicationRequestPurpose::getId,
            SwpApplicationRequestPurpose::getScheduleWorkProgrammeApplicationDetail,
            SwpApplicationRequestPurpose::getExtendTerm,
            SwpApplicationRequestPurpose::getExtendPhaseOrTerm,
            SwpApplicationRequestPurpose::getAmendWorkProgramme
        )
        .containsExactly(
            swpApplicationRequestPurpose.getId(),
            scheduleWorkProgrammeApplicationDetail,
            true,
            false,
            false
        );
  }

  @Test
  void setRequestPurposes_SetSingleRequestPurpose_ExtendAPhaseOrTerm() {
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    Set<SwpApplicationRequestPurposeOption> options = EnumSet.of(SwpApplicationRequestPurposeOption.EXTEND_A_PHASE_OR_TERM);

    underTest.setRequestPurposes(swpApplicationRequestPurpose, options);

    assertThat(swpApplicationRequestPurpose.getExtendPhaseOrTerm()).isTrue();
    assertThat(swpApplicationRequestPurpose.getExtendTerm()).isFalse();
    assertThat(swpApplicationRequestPurpose.getAmendWorkProgramme()).isFalse();
  }

  @Test
  void setRequestPurposes_SetSingleRequestPurpose_ExtendATerm() {
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    Set<SwpApplicationRequestPurposeOption> options = EnumSet.of(SwpApplicationRequestPurposeOption.EXTEND_A_TERM);

    underTest.setRequestPurposes(swpApplicationRequestPurpose, options);

    assertThat(swpApplicationRequestPurpose.getExtendPhaseOrTerm()).isFalse();
    assertThat(swpApplicationRequestPurpose.getExtendTerm()).isTrue();
    assertThat(swpApplicationRequestPurpose.getAmendWorkProgramme()).isFalse();
  }

  @Test
  void setRequestPurposes_SetSingleRequestPurpose_AmendTheWorkProgramme() {
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    Set<SwpApplicationRequestPurposeOption> options = EnumSet.of(SwpApplicationRequestPurposeOption.AMEND_THE_WORK_PROGRAMME);

    underTest.setRequestPurposes(swpApplicationRequestPurpose, options);

    assertThat(swpApplicationRequestPurpose.getExtendPhaseOrTerm()).isFalse();
    assertThat(swpApplicationRequestPurpose.getExtendTerm()).isFalse();
    assertThat(swpApplicationRequestPurpose.getAmendWorkProgramme()).isTrue();
  }

  @Test
  void setRequestPurposes_SetMultipleRequestPurposes() {
    Set<SwpApplicationRequestPurposeOption> options = EnumSet.of(
        SwpApplicationRequestPurposeOption.EXTEND_A_TERM,
        SwpApplicationRequestPurposeOption.AMEND_THE_WORK_PROGRAMME
    );
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();

    underTest.setRequestPurposes(swpApplicationRequestPurpose, options);

    assertThat(swpApplicationRequestPurpose.getExtendPhaseOrTerm()).isFalse();
    assertThat(swpApplicationRequestPurpose.getExtendTerm()).isTrue();
    assertThat(swpApplicationRequestPurpose.getAmendWorkProgramme()).isTrue();
  }

  @Test
  void setRequestPurposes_ResetsExistingFlags() {
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setExtendPhaseOrTerm(true);
    swpApplicationRequestPurpose.setExtendTerm(true);
    swpApplicationRequestPurpose.setAmendWorkProgramme(true);

    Set<SwpApplicationRequestPurposeOption> options = EnumSet.of(SwpApplicationRequestPurposeOption.EXTEND_A_TERM);
    underTest.setRequestPurposes(swpApplicationRequestPurpose, options);

    assertThat(swpApplicationRequestPurpose.getExtendPhaseOrTerm()).isFalse();
    assertThat(swpApplicationRequestPurpose.getExtendTerm()).isTrue();
    assertThat(swpApplicationRequestPurpose.getAmendWorkProgramme()).isFalse();
  }

  @Test
  void getFilledSwpApplicationRequestPurposeForm_RepositoryReturnsData_ReturnsFormWithRequestPurposes() {
    var detail = new ScheduleWorkProgrammeApplicationDetail();
    var persisted = new SwpApplicationRequestPurpose();
    persisted.setExtendPhaseOrTerm(true);
    persisted.setExtendTerm(false);
    persisted.setAmendWorkProgramme(true);

    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(detail))
        .thenReturn(Optional.of(persisted));

    var result = underTest.getFilledSwpApplicationRequestPurposeForm(detail);

    var expected = EnumSet.of(SwpApplicationRequestPurposeOption.EXTEND_A_PHASE_OR_TERM,
        SwpApplicationRequestPurposeOption.AMEND_THE_WORK_PROGRAMME);

    assertThat(result.getRequestPurposes()).isEqualTo(expected);
  }

  @Test
  void getFilledApplicationRequestPurposeForm_RepositoryReturnsEmpty_ReturnsEmptyForm() {
    var detail = new ScheduleWorkProgrammeApplicationDetail();

    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(detail))
        .thenReturn(Optional.empty());

    var result = underTest.getFilledSwpApplicationRequestPurposeForm(detail);

    assertThat(result.getRequestPurposes()).isEmpty();
  }

  @Test
  void getPersistedRequestPurposeOptions_AllFlagsFalse_ReturnsEmptySet() {
    var swpRequestPurpose = new SwpApplicationRequestPurpose();

    var result = underTest.getPersistedRequestPurposeOptions(swpRequestPurpose);

    assertThat(result).isEmpty();
  }

  @Test
  void getPersistedRequestPurposeOptions_ExtendPhaseOrTermTrue_ReturnsSetWithExtendAPhaseOrTerm() {
    var swpRequestPurpose = new SwpApplicationRequestPurpose();
    swpRequestPurpose.setExtendPhaseOrTerm(true);

    var result = underTest.getPersistedRequestPurposeOptions(swpRequestPurpose);

    assertThat(result).containsExactly(SwpApplicationRequestPurposeOption.EXTEND_A_PHASE_OR_TERM);
  }

  @Test
  void getPersistedRequestPurposeOptions_ExtendTermTrue_ReturnsSetWithExtendATerm() {
    var swpRequestPurpose = new SwpApplicationRequestPurpose();
    swpRequestPurpose.setExtendTerm(true);

    var result = underTest.getPersistedRequestPurposeOptions(swpRequestPurpose);

    assertThat(result).containsExactly(SwpApplicationRequestPurposeOption.EXTEND_A_TERM);
  }

  @Test
  void getPersistedRequestPurposeOptions_AmendWorkProgrammeTrue_ReturnsSetWithAmendTheWorkProgramme() {
    var swpRequestPurpose = new SwpApplicationRequestPurpose();
    swpRequestPurpose.setAmendWorkProgramme(true);

    var result = underTest.getPersistedRequestPurposeOptions(swpRequestPurpose);

    assertThat(result).containsExactly(SwpApplicationRequestPurposeOption.AMEND_THE_WORK_PROGRAMME);
  }

  @Test
  void getPersistedRequestPurposeOptions_MultipleFlagsTrue_ReturnsSetWithExpectedValues() {
    var swpRequestPurpose = new SwpApplicationRequestPurpose();
    swpRequestPurpose.setExtendPhaseOrTerm(true);
    swpRequestPurpose.setExtendTerm(true);
    swpRequestPurpose.setAmendWorkProgramme(true);

    var result = underTest.getPersistedRequestPurposeOptions(swpRequestPurpose);

    var expected = EnumSet.of(
        SwpApplicationRequestPurposeOption.EXTEND_A_PHASE_OR_TERM,
        SwpApplicationRequestPurposeOption.EXTEND_A_TERM,
        SwpApplicationRequestPurposeOption.AMEND_THE_WORK_PROGRAMME
    );
    assertThat(result).isEqualTo(expected);
  }
  @Test
  void saveOrUpdateRequestPurpose_RemovesPhaseOrTermExtension_TriggersCleanup() {
    var form = new SwpApplicationRequestPurposeForm();
    form.setRequestPurposes(Set.of(SwpApplicationRequestPurposeOption.AMEND_THE_WORK_PROGRAMME));

    var detail = new ScheduleWorkProgrammeApplicationDetail();
    var existingPurpose = new SwpApplicationRequestPurpose(UUID.randomUUID());

    existingPurpose.setExtendPhaseOrTerm(true);

    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(detail))
        .thenReturn(Optional.of(existingPurpose));

    underTest.saveOrUpdateRequestPurpose(detail, form);

    verify(swpApplicationRequestPurposeRepository).save(swpApplicationRequestPurposeArgumentCaptor.capture());
    assertThat(swpApplicationRequestPurposeArgumentCaptor.getValue().getExtendPhaseOrTerm()).isFalse();

    verify(licenceScheduleExtensionRepository).deleteByScheduleWorkProgrammeApplicationDetails(detail);

    verify(licenceScheduleSupportingInformationService).handleSupportingInformationExtensionRemoval(detail);
  }

  @Test
  void getPageOptions_whenWorkProgrammeButNoAmendableActivities_excludesAmendOption() {
    var detail = new ScheduleWorkProgrammeApplicationDetail();
    var licence = mock(Licence.class);
    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);
    scheduleWorkProgrammeApplication.setLicenceSchedule(licenceSchedule);
    detail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);
    when(licence.getType()).thenReturn(LicenceType.SEAWARD_EXPLORATION);
    when(licenceTypeRulesResolver.hasTerms(LicenceType.SEAWARD_EXPLORATION)).thenReturn(true);
    when(licenceTypeRulesResolver.arePhasesCaptured(LicenceType.SEAWARD_EXPLORATION)).thenReturn(true);
    when(licenceTypeRulesResolver.hasWorkProgramme(LicenceType.SEAWARD_EXPLORATION)).thenReturn(true);
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(detail)).thenReturn(mock(LicenceScheduleDetail.class));
    when(licenceScheduleService.hasCurrentWorkProgrammeActivities(any())).thenReturn(false);

    var options = underTest.getPageOptions(detail);

    assertThat(options).containsExactly(SwpApplicationRequestPurposeOption.EXTEND_A_PHASE_OR_TERM);
  }

  @Test
  void getPageOptions_whenWorkProgrammeAndAmendableActivities_includesAmendOption() {
    var detail = new ScheduleWorkProgrammeApplicationDetail();
    var licence = mock(Licence.class);
    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);
    scheduleWorkProgrammeApplication.setLicenceSchedule(licenceSchedule);
    detail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);
    when(licence.getType()).thenReturn(LicenceType.SEAWARD_EXPLORATION);
    when(licenceTypeRulesResolver.hasTerms(LicenceType.SEAWARD_EXPLORATION)).thenReturn(true);
    when(licenceTypeRulesResolver.arePhasesCaptured(LicenceType.SEAWARD_EXPLORATION)).thenReturn(true);
    when(licenceTypeRulesResolver.hasWorkProgramme(LicenceType.SEAWARD_EXPLORATION)).thenReturn(true);
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(detail)).thenReturn(mock(LicenceScheduleDetail.class));
    when(licenceScheduleService.hasCurrentWorkProgrammeActivities(any())).thenReturn(true);

    var options = underTest.getPageOptions(detail);

    assertThat(options).contains(SwpApplicationRequestPurposeOption.AMEND_THE_WORK_PROGRAMME);
  }

  @Test
  void hasAmendableWorkProgrammeActivities_whenCurrentActivities_returnsTrue() {
    var detail = new ScheduleWorkProgrammeApplicationDetail();
    var scheduleDetail = mock(LicenceScheduleDetail.class);
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(detail))
        .thenReturn(scheduleDetail);
    when(licenceScheduleService.hasCurrentWorkProgrammeActivities(scheduleDetail)).thenReturn(true);

    assertThat(underTest.hasAmendableWorkProgrammeActivities(detail)).isTrue();
  }

  @Test
  void hasAmendableWorkProgrammeActivities_whenNoCurrentActivities_returnsFalse() {
    var detail = new ScheduleWorkProgrammeApplicationDetail();
    var scheduleDetail = mock(LicenceScheduleDetail.class);
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(detail))
        .thenReturn(scheduleDetail);
    when(licenceScheduleService.hasCurrentWorkProgrammeActivities(scheduleDetail)).thenReturn(false);

    assertThat(underTest.hasAmendableWorkProgrammeActivities(detail)).isFalse();
  }

  @Test
  void applyDefaultRequestPurposeIfNotApplicable_whenNoAmendableActivities_persistsExtendOption() {
    var detail = new ScheduleWorkProgrammeApplicationDetail();
    var licence = mock(Licence.class);
    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);
    scheduleWorkProgrammeApplication.setLicenceSchedule(licenceSchedule);
    detail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);
    when(licence.getType()).thenReturn(LicenceType.SEAWARD_EXPLORATION);
    when(licenceTypeRulesResolver.hasTerms(LicenceType.SEAWARD_EXPLORATION)).thenReturn(true);
    when(licenceTypeRulesResolver.arePhasesCaptured(LicenceType.SEAWARD_EXPLORATION)).thenReturn(true);
    when(licenceTypeRulesResolver.hasWorkProgramme(LicenceType.SEAWARD_EXPLORATION)).thenReturn(true);
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(detail)).thenReturn(mock(LicenceScheduleDetail.class));
    when(licenceScheduleService.hasCurrentWorkProgrammeActivities(any())).thenReturn(false);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(detail))
        .thenReturn(Optional.empty());

    underTest.applyDefaultRequestPurposeIfNotApplicable(detail);

    verify(swpApplicationRequestPurposeRepository).save(swpApplicationRequestPurposeArgumentCaptor.capture());
    var saved = swpApplicationRequestPurposeArgumentCaptor.getValue();
    assertThat(saved.getExtendPhaseOrTerm()).isTrue();
    assertThat(saved.getAmendWorkProgramme()).isFalse();
  }

  @Test
  void applyDefaultRequestPurposeIfNotApplicable_whenAmendableActivities_doesNotPersist() {
    var detail = new ScheduleWorkProgrammeApplicationDetail();
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(detail)).thenReturn(mock(LicenceScheduleDetail.class));
    when(licenceScheduleService.hasCurrentWorkProgrammeActivities(any())).thenReturn(true);

    underTest.applyDefaultRequestPurposeIfNotApplicable(detail);

    verify(swpApplicationRequestPurposeRepository, never()).save(any());
  }
}
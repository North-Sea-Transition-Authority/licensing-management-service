package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationWpaRequirementServiceTest {

  @Mock
  private LicenceContinuationWpaRequirementRepository licenceContinuationWpaRequirementRepository;

  @InjectMocks
  private LicenceContinuationWpaRequirementService licenceContinuationWpaRequirementService;

  @Captor
  private ArgumentCaptor<LicenceContinuationWpaRequirementRequest> licenceContinuationWpaRequirementRequestArgumentCaptor;

  @Test
  void save_WhenCompleted_ClearsActionsAndSavesFurtherInfo() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    var licenceContinuationWpaRequirementForm = new LicenceContinuationWpaRequirementForm();
    licenceContinuationWpaRequirementForm.setWorkProgrammeActivitiesCompletionStatus(true);
    licenceContinuationWpaRequirementForm.setFurtherInformation("Info");
    licenceContinuationWpaRequirementForm.setActionsToCompleteWorkProgrammeActivities("Should be cleared");

    when(
        licenceContinuationWpaRequirementRepository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(new LicenceContinuationWpaRequirementRequest()));

    licenceContinuationWpaRequirementService.saveLicenceContinuationWorkProgrammeActivitiesRequirementForm(licenceContinuationWpaRequirementForm, licenceContinuationApplicationDetail);

    verify(licenceContinuationWpaRequirementRepository).save(licenceContinuationWpaRequirementRequestArgumentCaptor.capture());

    var savedRequest = licenceContinuationWpaRequirementRequestArgumentCaptor.getValue();
    assertThat(savedRequest.getWorkProgrammeActivitiesCompletionStatus()).isTrue();
    assertThat(savedRequest.getFurtherInformation()).isEqualTo("Info");
    assertThat(savedRequest.getActionsToCompleteWorkProgrammeActivities()).isNull();
  }

  @Test
  void save_WhenNotCompleted_ClearsFurtherInfoAndSavesActions() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    var licenceContinuationWpaRequirementForm = new LicenceContinuationWpaRequirementForm();
    licenceContinuationWpaRequirementForm.setWorkProgrammeActivitiesCompletionStatus(false);
    licenceContinuationWpaRequirementForm.setFurtherInformation("Should be cleared");
    licenceContinuationWpaRequirementForm.setActionsToCompleteWorkProgrammeActivities("Actions needed");

    when(
        licenceContinuationWpaRequirementRepository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(new LicenceContinuationWpaRequirementRequest()));

    licenceContinuationWpaRequirementService.saveLicenceContinuationWorkProgrammeActivitiesRequirementForm(licenceContinuationWpaRequirementForm, licenceContinuationApplicationDetail);

    verify(licenceContinuationWpaRequirementRepository).save(licenceContinuationWpaRequirementRequestArgumentCaptor.capture());

    var savedRequest = licenceContinuationWpaRequirementRequestArgumentCaptor.getValue();
    assertThat(savedRequest.getWorkProgrammeActivitiesCompletionStatus()).isFalse();
    assertThat(savedRequest.getFurtherInformation()).isNull();
    assertThat(savedRequest.getActionsToCompleteWorkProgrammeActivities()).isEqualTo("Actions needed");
  }

  @Test
  void getForm_WhenExists_ReturnsMappedForm() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    var licenceContinuationWpaRequirementRequest = new LicenceContinuationWpaRequirementRequest();
    licenceContinuationWpaRequirementRequest.setWorkProgrammeActivitiesCompletionStatus(true);
    licenceContinuationWpaRequirementRequest.setFurtherInformation("Info");

    when(
        licenceContinuationWpaRequirementRepository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail)).thenReturn(Optional.of(licenceContinuationWpaRequirementRequest));

    var result = licenceContinuationWpaRequirementService.getLicenceContinuationWorkProgrammeActivitiesRequirementForm(licenceContinuationApplicationDetail);

    assertThat(result.getWorkProgrammeActivitiesCompletionStatus()).isTrue();
    assertThat(result.getFurtherInformation()).isEqualTo("Info");
  }

  @Test
  void getForm_WhenNotExists_ReturnsEmptyForm() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    when(
        licenceContinuationWpaRequirementRepository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail)).thenReturn(Optional.empty());

    var result = licenceContinuationWpaRequirementService.getLicenceContinuationWorkProgrammeActivitiesRequirementForm(licenceContinuationApplicationDetail);

    assertThat(result).isNotNull();
    assertThat(result.getWorkProgrammeActivitiesCompletionStatus()).isNull();
  }
}
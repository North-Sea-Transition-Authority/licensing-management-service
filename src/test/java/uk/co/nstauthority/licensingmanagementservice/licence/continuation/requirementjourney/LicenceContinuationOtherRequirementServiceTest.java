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
class LicenceContinuationOtherRequirementServiceTest {

  @Mock
  private LicenceContinuationOtherRequirementRepository repository;

  @Captor
  private ArgumentCaptor<LicenceContinuationOtherRequirementRequest> captor;

  @InjectMocks
  private LicenceContinuationOtherRequirementService service;

  @Test
  void save_WhenEvidenceSubmittedIsTrue_ClearsActions() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    var licenceContinuationOtherRequirementForm = new LicenceContinuationOtherRequirementForm();
    licenceContinuationOtherRequirementForm.setFinancialCapacityEvidenceSubmissionStatus(true);
    licenceContinuationOtherRequirementForm.setActionsToProvideFinancialEvidence("Should be cleared");

    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(new LicenceContinuationOtherRequirementRequest()));

    service.saveLicenceContinuationOtherRequirementForm(licenceContinuationOtherRequirementForm, licenceContinuationApplicationDetail);

    verify(repository).save(captor.capture());

    var savedRequest = captor.getValue();
    assertThat(savedRequest.getFinancialCapacityEvidenceSubmissionStatus()).isTrue();
    assertThat(savedRequest.getActionsToProvideFinancialEvidence()).isNull();
  }

  @Test
  void save_WhenEvidenceSubmittedIsFalse_SavesActions() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    var licenceContinuationOtherRequirementForm = new LicenceContinuationOtherRequirementForm();
    licenceContinuationOtherRequirementForm.setFinancialCapacityEvidenceSubmissionStatus(false);
    licenceContinuationOtherRequirementForm.setActionsToProvideFinancialEvidence("Actions needed");

    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(new LicenceContinuationOtherRequirementRequest()));

    service.saveLicenceContinuationOtherRequirementForm(licenceContinuationOtherRequirementForm, licenceContinuationApplicationDetail);

    verify(repository).save(captor.capture());

    var savedRequest = captor.getValue();
    assertThat(savedRequest.getFinancialCapacityEvidenceSubmissionStatus()).isFalse();
    assertThat(savedRequest.getActionsToProvideFinancialEvidence()).isEqualTo("Actions needed");
  }

  @Test
  void getForm_WhenExists_ReturnsMappedForm() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    var licenceContinuationOtherRequirementRequest = new LicenceContinuationOtherRequirementRequest();
    licenceContinuationOtherRequirementRequest.setFinancialCapacityEvidenceSubmissionStatus(true);
    licenceContinuationOtherRequirementRequest.setActionsToProvideFinancialEvidence("Actions");

    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail)).thenReturn(Optional.of(licenceContinuationOtherRequirementRequest));

    var result = service.getLicenceContinuationOtherRequirementForm(licenceContinuationApplicationDetail);

    assertThat(result.getFinancialCapacityEvidenceSubmissionStatus()).isTrue();
    assertThat(result.getActionsToProvideFinancialEvidence()).isEqualTo("Actions");
  }

  @Test
  void getForm_WhenNotExists_ReturnsEmptyForm() {
    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail)).thenReturn(Optional.empty());

    var result = service.getLicenceContinuationOtherRequirementForm(licenceContinuationApplicationDetail);

    assertThat(result).isNotNull();
    assertThat(result.getFinancialCapacityEvidenceSubmissionStatus()).isNull();
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationOtherRequirementServiceTest {

  @Mock
  private LicenceContinuationOtherRequirementRepository repository;

  @Mock
  private ApplicationFileService applicationFileService;

  @Captor
  private ArgumentCaptor<LicenceContinuationOtherRequirementRequest> captor;

  @InjectMocks
  private LicenceContinuationOtherRequirementService service;

  @Test
  void save_WhenEvidenceSubmittedIsTrue_ClearsActions() {
    var licenceContinuationApplicationDetail = detailWithId();
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
    var licenceContinuationApplicationDetail = detailWithId();
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
  void save_WhenDevelopmentConsentGrantedIsTrue_ClearsActions() {
    var licenceContinuationApplicationDetail = detailWithId();
    var form = new LicenceContinuationOtherRequirementForm();
    form.setDevelopmentConsentGrantStatus(true);
    form.setActionsToApproveDevelopmentConsent("Should be cleared");

    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(new LicenceContinuationOtherRequirementRequest()));

    service.saveLicenceContinuationOtherRequirementForm(form, licenceContinuationApplicationDetail);

    verify(repository).save(captor.capture());

    var savedRequest = captor.getValue();
    assertThat(savedRequest.getDevelopmentConsentGrantStatus()).isTrue();
    assertThat(savedRequest.getActionsToApproveDevelopmentConsent()).isNull();
  }

  @Test
  void save_WhenDevelopmentConsentGrantedIsFalse_SavesActions() {
    var licenceContinuationApplicationDetail = detailWithId();
    var form = new LicenceContinuationOtherRequirementForm();
    form.setDevelopmentConsentGrantStatus(false);
    form.setActionsToApproveDevelopmentConsent("Consent Actions needed");

    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(new LicenceContinuationOtherRequirementRequest()));

    service.saveLicenceContinuationOtherRequirementForm(form, licenceContinuationApplicationDetail);

    verify(repository).save(captor.capture());

    var savedRequest = captor.getValue();
    assertThat(savedRequest.getDevelopmentConsentGrantStatus()).isFalse();
    assertThat(savedRequest.getActionsToApproveDevelopmentConsent()).isEqualTo("Consent Actions needed");
  }

  @Test
  void save_WhenRelinquishmentPerformedIsTrue_ClearsActions() {
    var licenceContinuationApplicationDetail = detailWithId();
    var form = new LicenceContinuationOtherRequirementForm();
    form.setRelinquishmentRequirementStatus(true);
    form.setActionsToRelinquishRequiredLicenceArea("Should be cleared");

    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(new LicenceContinuationOtherRequirementRequest()));

    service.saveLicenceContinuationOtherRequirementForm(form, licenceContinuationApplicationDetail);

    verify(repository).save(captor.capture());

    var savedRequest = captor.getValue();
    assertThat(savedRequest.getRelinquishmentRequirementStatus()).isTrue();
    assertThat(savedRequest.getActionsToRelinquishRequiredLicenceArea()).isNull();
  }

  @Test
  void save_WhenRelinquishmentPerformedIsFalse_SavesActions() {
    var licenceContinuationApplicationDetail = detailWithId();
    var form = new LicenceContinuationOtherRequirementForm();
    form.setRelinquishmentRequirementStatus(false);
    form.setActionsToRelinquishRequiredLicenceArea("Relinquishment actions underway");

    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(new LicenceContinuationOtherRequirementRequest()));

    service.saveLicenceContinuationOtherRequirementForm(form, licenceContinuationApplicationDetail);

    verify(repository).save(captor.capture());

    var savedRequest = captor.getValue();
    assertThat(savedRequest.getRelinquishmentRequirementStatus()).isFalse();
    assertThat(savedRequest.getActionsToRelinquishRequiredLicenceArea()).isEqualTo("Relinquishment actions underway");
  }

  @Test
  void getForm_WhenExists_ReturnsMappedForm() {
    var licenceContinuationApplicationDetail = detailWithId();
    var request = new LicenceContinuationOtherRequirementRequest();
    request.setFinancialCapacityEvidenceSubmissionStatus(true);
    request.setActionsToProvideFinancialEvidence("Actions");
    request.setDevelopmentConsentGrantStatus(true);
    request.setActionsToApproveDevelopmentConsent("Consent Actions");
    request.setRelinquishmentRequirementStatus(false);
    request.setActionsToRelinquishRequiredLicenceArea("Relinquishment Actions");

    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail)).thenReturn(Optional.of(request));

    var result = service.getLicenceContinuationOtherRequirementForm(licenceContinuationApplicationDetail);

    assertThat(result.getFinancialCapacityEvidenceSubmissionStatus()).isTrue();
    assertThat(result.getActionsToProvideFinancialEvidence()).isEqualTo("Actions");
    assertThat(result.getDevelopmentConsentGrantStatus()).isTrue();
    assertThat(result.getActionsToApproveDevelopmentConsent()).isEqualTo("Consent Actions");
    assertThat(result.getRelinquishmentRequirementStatus()).isFalse();
    assertThat(result.getActionsToRelinquishRequiredLicenceArea()).isEqualTo("Relinquishment Actions");
  }

  private static LicenceContinuationApplicationDetail detailWithId() {
    var detail = new LicenceContinuationApplicationDetail();
    detail.setId(UUID.randomUUID());
    return detail;
  }

  @Test
  void getForm_WhenNotExists_ReturnsEmptyForm() {
    var licenceContinuationApplicationDetail = detailWithId();
    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail)).thenReturn(Optional.empty());

    var result = service.getLicenceContinuationOtherRequirementForm(licenceContinuationApplicationDetail);

    assertThat(result).isNotNull();
    assertThat(result.getFinancialCapacityEvidenceSubmissionStatus()).isNull();
    assertThat(result.getDevelopmentConsentGrantStatus()).isNull();
    assertThat(result.getRelinquishmentRequirementStatus()).isNull();
  }
}
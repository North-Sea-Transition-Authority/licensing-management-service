package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportalapi.generated.types.Subarea;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationLicenceOperatorsServiceTest {

  @Mock
  private LicenceContinuationLicenceOperatorsRepository repository;

  @InjectMocks
  private LicenceContinuationLicenceOperatorsService service;

  @Captor
  private ArgumentCaptor<LicenceContinuationLicenceOperatorsRequest> requestCaptor;

  @Test
  void hasMissingOperators_returnsTrue() {
    var subareaWithOp = new Subarea();
    subareaWithOp.setOperator(new uk.co.fivium.energyportalapi.generated.types.OrganisationUnit());

    var subareaWithoutOp = new Subarea();
    subareaWithoutOp.setOperator(null);

    assertThat(service.hasMissingOperators(List.of(subareaWithOp, subareaWithoutOp))).isTrue();
  }

  @Test
  void saveForm_whenMissingOperatorsIsTrue() {
    var applicationDetail = new LicenceContinuationApplicationDetail();
    var form = new LicenceContinuationLicenceOperatorsForm();
    form.setPendingActionsExplanation("test");

    when(repository.findByLicenceContinuationApplicationDetail(applicationDetail)).thenReturn(Optional.empty());

    service.saveLicenceContinuationLicenceOperatorsForm(form, applicationDetail);

    verify(repository).save(requestCaptor.capture());
    var savedRequest = requestCaptor.getValue();

    assertThat(savedRequest.getPendingActionsExplanation()).isEqualTo("test");
    assertThat(savedRequest.getLicenceContinuationApplicationDetail()).isEqualTo(applicationDetail);
  }

  @Test
  void saveForm_whenMissingOperatorsIsFalse() {
    var applicationDetail = new LicenceContinuationApplicationDetail();
    var existingRequest = new LicenceContinuationLicenceOperatorsRequest();
    existingRequest.setPendingActionsExplanation("Old explanation");

    var form = new LicenceContinuationLicenceOperatorsForm();

    when(repository.findByLicenceContinuationApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(existingRequest));

    service.saveLicenceContinuationLicenceOperatorsForm(form, applicationDetail);

    verify(repository).save(requestCaptor.capture());
    var savedRequest = requestCaptor.getValue();

    assertThat(savedRequest.getPendingActionsExplanation()).isNull();
  }

  @Test
  void getForm_ExistingData() {
    var applicationDetail = new LicenceContinuationApplicationDetail();
    var existingRequest = new LicenceContinuationLicenceOperatorsRequest();
    existingRequest.setPendingActionsExplanation("Expected explanation");

    when(repository.findByLicenceContinuationApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(existingRequest));

    var form = service.getLicenceContinuationLicenceOperatorsForm(applicationDetail);

    assertThat(form.getPendingActionsExplanation()).isEqualTo("Expected explanation");
  }
}
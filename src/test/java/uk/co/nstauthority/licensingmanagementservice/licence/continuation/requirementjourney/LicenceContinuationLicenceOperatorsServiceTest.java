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
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
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
  void hasMissingOperators_whenOperatorIsNoOperatorOrganisation_thenTrue() {
    var subareaWithOperator = subareaWithOperatorNamed("Shell UK Limited");
    var subareaWithNoOperator = subareaWithOperatorNamed("NO OPERATOR");

    var result = service.hasMissingOperators(List.of(subareaWithOperator, subareaWithNoOperator));

    assertThat(result).isTrue();
  }

  @Test
  void hasMissingOperators_whenOperatorIsNotReturned_thenTrue() {
    var subareaWithOperator = subareaWithOperatorNamed("Shell UK Limited");
    var subareaWithoutOperator = new Subarea();

    var result = service.hasMissingOperators(List.of(subareaWithOperator, subareaWithoutOperator));

    assertThat(result).isTrue();
  }

  @Test
  void hasMissingOperators_whenAllSubareasHaveAnOperator_thenFalse() {
    var firstSubarea = subareaWithOperatorNamed("Shell UK Limited");
    var secondSubarea = subareaWithOperatorNamed("BP Exploration Operating Company Limited");

    var result = service.hasMissingOperators(List.of(firstSubarea, secondSubarea));

    assertThat(result).isFalse();
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

  private static Subarea subareaWithOperatorNamed(String operatorName) {
    var operator = new OrganisationUnit();
    operator.setName(operatorName);
    var subarea = new Subarea();
    subarea.setOperator(operator);
    return subarea;
  }
}
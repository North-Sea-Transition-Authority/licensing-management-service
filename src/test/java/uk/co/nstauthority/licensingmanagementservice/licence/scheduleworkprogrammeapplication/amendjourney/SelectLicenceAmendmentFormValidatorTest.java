package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class SelectLicenceAmendmentFormValidatorTest {

  @InjectMocks
  SelectLicenceAmendmentFormValidator validator;

  @Mock
  LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  @Test
  void isValid() {
    var form = new SelectLicenceAmendmentForm();
    form.setSelectedWorkProgrammeActivityAmendmentId(UUID.randomUUID());
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertTrue(validator.isValid(form,bindingResult));
  }


  @Test
  void isValid_invalidForm() {
    var form = new SelectLicenceAmendmentForm();
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertFalse(validator.isValid(form,bindingResult));
  }


  @Test
  void isValid_invalidForm_duplicateAmendment() {
    var form = new SelectLicenceAmendmentForm();
    when(licenceWorkProgrammeAmendmentRepository.existsByWorkProgrammeActivityId(any())).thenReturn(true);
    form.setSelectedWorkProgrammeActivityAmendmentId(UUID.randomUUID());
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertFalse(validator.isValid(form,bindingResult));
  }
}
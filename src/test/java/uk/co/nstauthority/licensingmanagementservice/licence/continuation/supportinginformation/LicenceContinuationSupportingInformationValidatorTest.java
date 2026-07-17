package uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;

class LicenceContinuationSupportingInformationValidatorTest {

  private LicenceContinuationSupportingInformationValidator validator;

  @BeforeEach
  void setUp() {
    validator = new LicenceContinuationSupportingInformationValidator();
  }

  @Test
  void isValid_whenQuestionNotAnswered_rejectsAsRequired() {
    var form = new LicenceContinuationSupportingInformationForm();
    Errors errors = new BeanPropertyBindingResult(form, "form");

    assertThat(validator.isValid(form, errors)).isFalse();
    assertThat(errors.getFieldError("hasAdditionalSupportingInformation"))
        .isNotNull()
        .extracting(DefaultMessageSourceResolvable::getCode)
        .isEqualTo("hasAdditionalSupportingInformation.required");
  }

  @Test
  void isValid_whenYesAndNoDocuments_rejectsDocumentsAsRequired() {
    var form = new LicenceContinuationSupportingInformationForm();
    form.setHasAdditionalSupportingInformation(true);
    Errors errors = new BeanPropertyBindingResult(form, "form");

    assertThat(validator.isValid(form, errors)).isFalse();
    assertThat(errors.getFieldError("documents"))
        .isNotNull()
        .extracting(DefaultMessageSourceResolvable::getCode)
        .isEqualTo("documents.required");
  }

  @Test
  void isValid_whenYesAndDocumentsUploaded_hasNoErrors() {
    var form = new LicenceContinuationSupportingInformationForm();
    form.setHasAdditionalSupportingInformation(true);
    form.setDocuments(List.of(new UploadedFileForm()));
    Errors errors = new BeanPropertyBindingResult(form, "form");

    assertThat(validator.isValid(form, errors)).isTrue();
  }

  @Test
  void isValid_whenNo_hasNoErrors() {
    var form = new LicenceContinuationSupportingInformationForm();
    form.setHasAdditionalSupportingInformation(false);
    Errors errors = new BeanPropertyBindingResult(form, "form");

    assertThat(validator.isValid(form, errors)).isTrue();
  }
}

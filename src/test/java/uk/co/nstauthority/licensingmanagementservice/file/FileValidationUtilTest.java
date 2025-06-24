package uk.co.nstauthority.licensingmanagementservice.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.form.XyzApplicationForm;

class FileValidationUtilTest {

  @Test
  void validateFilesHaveDescriptions_validForms() {
    var uploadedFileForms = FileUploadTestUtil.validDocumentForms;
    var form = new XyzApplicationForm();
    form.setDocuments(uploadedFileForms);

    var errors = new BeanPropertyBindingResult(form, "form");

    FileValidationUtil.validator().validate(errors, uploadedFileForms, "documents");

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validateFilesHaveDescriptions_noDescription() {
    var uploadedFileForms = FileUploadTestUtil.documentFormsWithMissingDescription;
    var form = new XyzApplicationForm();
    form.setDocuments(uploadedFileForms);

    var errors = new BeanPropertyBindingResult(form, "form");

    FileValidationUtil.validator().validate(errors, uploadedFileForms, "documents");

    assertThat(errors.hasErrors()).isTrue();
    assertThat(ValidatorTestingUtil.extractErrorMessages(errors))
        .containsExactly(
            entry("documents[1].uploadedFileDescription", Set.of("Enter a file description"))
        );
  }

  @Test
  void validateFiles_withNotEnoughFiles() {
    var uploadedFileForms = FileUploadTestUtil.validDocumentForms;
    var form = new XyzApplicationForm();
    form.setDocuments(uploadedFileForms);

    var errors = new BeanPropertyBindingResult(form, "form");

    FileValidationUtil.validator()
        .withMinimumNumberOfFiles(3, "Upload at least three documents")
        .validate(errors, uploadedFileForms, "documents");

    assertThat(errors.hasErrors()).isTrue();
    assertThat(ValidatorTestingUtil.extractErrorMessages(errors))
        .containsExactly(
            entry("documents", Set.of("Upload at least three documents"))
        );
  }

  @Test
  void validateFiles_withTooManyFiles() {
    var uploadedFileForms = FileUploadTestUtil.validDocumentForms;
    var form = new XyzApplicationForm();
    form.setDocuments(uploadedFileForms);

    var errors = new BeanPropertyBindingResult(form, "form");

    FileValidationUtil.validator()
        .withMaximumNumberOfFiles(1, "Upload at most one document")
        .validate(errors, uploadedFileForms, "documents");

    assertThat(errors.hasErrors()).isTrue();
    assertThat(ValidatorTestingUtil.extractErrorMessages(errors))
        .containsExactly(
            entry("documents", Set.of("Upload at most one document"))
        );
  }
}

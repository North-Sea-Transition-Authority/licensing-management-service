package uk.co.nstauthority.template.file;

import java.util.List;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.nstauthority.template.util.IllegalUtilClassInstantiationException;

public class FileValidationUtil {

  private static final String FILES_EMPTY_ERROR_CODE = "%s.belowThreshold";
  private static final String FILES_TOO_MANY_ERROR_CODE = "%s.limitExceeded";

  private FileValidationUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  public static Validator validator() {
    return new Validator();
  }

  public static class Validator {

    private boolean descriptionsRequired = true;

    private boolean checkMinNumberOfFiles = false;
    private int maximumNumberOfFiles = Integer.MAX_VALUE;
    private String maxErrorMessage;

    private boolean checkMaxNumberOfFiles = false;
    private int minimumNumberOfFiles = 0;
    private String minErrorMessage;

    private Validator() {
    }

    public Validator withMandatoryDescriptions(boolean descriptionsRequired) {
      this.descriptionsRequired = descriptionsRequired;
      return this;
    }

    public Validator withMaximumNumberOfFiles(int maxFileCount, String errorMessage) {
      this.checkMaxNumberOfFiles = true;
      this.maximumNumberOfFiles = maxFileCount;
      this.maxErrorMessage = errorMessage;
      return this;
    }

    public Validator withMinimumNumberOfFiles(int minFileCount, String errorMessage) {
      this.checkMinNumberOfFiles = true;
      this.minimumNumberOfFiles = minFileCount;
      this.minErrorMessage = errorMessage;
      return this;
    }

    public void validate(Errors errors, List<UploadedFileForm> fileUploadForms, String fieldName) {

      if (fileUploadForms != null && descriptionsRequired) {
        for (var i = 0; i < fileUploadForms.size(); i++) {
          var field = "%s[%s].uploadedFileDescription".formatted(fieldName, i);
          ValidationUtils.rejectIfEmptyOrWhitespace(errors, field, "mandatory", "Enter a file description");
        }
      }

      if (checkMinNumberOfFiles && fileUploadForms != null && minimumNumberOfFiles > fileUploadForms.size()) {
        errors.rejectValue(
            fieldName,
            FILES_EMPTY_ERROR_CODE.formatted(fieldName),
            minErrorMessage
        );
      }

      if (checkMaxNumberOfFiles && fileUploadForms != null && fileUploadForms.size() > maximumNumberOfFiles) {
        errors.rejectValue(
            fieldName,
            FILES_TOO_MANY_ERROR_CODE.formatted(fieldName),
            maxErrorMessage
        );
      }
    }
  }
}

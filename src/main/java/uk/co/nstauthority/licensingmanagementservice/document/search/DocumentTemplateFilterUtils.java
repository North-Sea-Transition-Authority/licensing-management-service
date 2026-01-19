package uk.co.nstauthority.licensingmanagementservice.document.search;

import io.micrometer.common.util.StringUtils;
import java.util.List;
import java.util.Objects;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.EnumValidationUtil;

public class DocumentTemplateFilterUtils {

  private DocumentTemplateFilterUtils() {
    throw new IllegalStateException("Cannot instantiate DocumentTemplateFilterUtils as it's a utils class");
  }

  public static boolean filterLicenceType(LmsDocumentTemplateDto dataItemDto, List<String> licenceTypes) {
    if (Objects.isNull(dataItemDto.licenceType())
        || EnumValidationUtil.containsInvalidEnumValue(LicenceType.class, licenceTypes)) {
      return true;
    }
    return licenceTypes.contains(dataItemDto.licenceType().getEnumName());
  }

  public static boolean filterDocumentTitle(LmsDocumentTemplateDto dataItemDto, String title) {
    return StringUtils.isBlank(title) || dataItemDto.title().toLowerCase().contains(title.toLowerCase());
  }
}

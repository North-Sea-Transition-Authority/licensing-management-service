package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.UUID;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

public record LmsDocumentTemplateDto(
    UUID documentTemplateId,
    String title,
    String description,
    LicenceType licenceType,
    ApplicationType applicationType,
    int displayOrder,
    String documentTemplateUrl
) {
  public static LmsDocumentTemplateDto from(DocumentTemplateMetadata metadata,
                                            DocumentTemplateDto documentTemplateDto,
                                            String url) {
    return new LmsDocumentTemplateDto(
        documentTemplateDto.id(),
        documentTemplateDto.title(),
        documentTemplateDto.description(),
        metadata.getLicenceType(),
        metadata.getApplicationType(),
        documentTemplateDto.displayOrder(),
        url
    );
  }
}
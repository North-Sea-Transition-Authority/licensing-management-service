package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;

@Service
public class ApplicationLetterService {

  private final DocumentInstanceService documentInstanceService;
  private final DocumentInstanceSectionService documentInstanceSectionService;
  private final DocumentTemplateService documentTemplateService;

  @Autowired
  public ApplicationLetterService(
      DocumentInstanceService documentInstanceService,
      DocumentInstanceSectionService documentInstanceSectionService,
      DocumentTemplateService documentTemplateService
  ) {
    this.documentInstanceService = documentInstanceService;
    this.documentInstanceSectionService = documentInstanceSectionService;
    this.documentTemplateService = documentTemplateService;
  }

  @Transactional
  public DocumentInstanceDto createDocumentInstance(LicenceApplication application) {
    String itemReference = application.getId().toString();

    DocumentTemplateType templateType = switch (application.getApplicationType()) {
      case CONTINUATION_APPLICATION -> DocumentTemplateType.CONTINUATION_LETTER;
      case SCHEDULE_AMENDMENT_APPLICATION -> DocumentTemplateType.EXTENSION_APPROVAL_LETTER;
    };

    String templateMnemonic = "%s-%s".formatted(templateType.name(), application.getApplicationType().name());
    DocumentTemplateDto template = documentTemplateService.getDocumentTemplateDtoByMnemonicOrThrow(templateMnemonic);

    return documentInstanceService.createDocumentInstance(
        itemReference,
        application.getApplicationType().name(),
        template.title(),
        template.description(),
        template
    );
  }

  public DocumentInstanceDto getDocumentInstance(LicenceApplication application) {
    String itemReference = application.getId().toString();

    var existingDocs = documentInstanceService.getDocumentInstanceDtosByItemReference(itemReference);

    return existingDocs.stream()
        .findFirst()
        .orElseThrow(
            () -> new NoSuchElementException(
                "No document instance found for application with id %s".formatted(application.getId())
            ));
  }

  public List<DocumentInstanceDto> getApplicationLetters(List<LicenceApplication> applications) {
    if (CollectionUtils.isEmpty(applications)) {
      return List.of();
    }

    var applicationIds = applications.stream()
        .map(app -> app.getId().toString())
        .toList();
        
    return documentInstanceService.getDocumentInstanceDtosByItemReferences(applicationIds);
  }

  public String getApplicationLetterTitle(LicenceApplication application) {
    return documentInstanceService.getDocumentInstanceDtosByItemReference(application.getId().toString()).stream()
        .findFirst()
        .map(DocumentInstanceDto::title)
        .orElseThrow(() -> new NoSuchElementException("No document instance found for application with id %s"
            .formatted(application.getId())));
  }

  public DocumentInstanceSectionDto getParentDocumentSectionDto(
      AddSectionOption addSectionOption,
      DocumentInstanceSectionDto currentDocumentSection
  ) {
    return switch (addSectionOption) {
      case ADD_BEFORE,
           ADD_AFTER -> currentDocumentSection.parentId() != null
                        ? documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(currentDocumentSection.parentId())
                        : null;
      case ADD_SUBSECTION -> currentDocumentSection;
    };
  }
}
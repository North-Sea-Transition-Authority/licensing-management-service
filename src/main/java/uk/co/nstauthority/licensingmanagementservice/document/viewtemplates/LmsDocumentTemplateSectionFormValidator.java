package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionFormValidator;

@Service
public class LmsDocumentTemplateSectionFormValidator {

  private final DocumentTemplateSectionFormValidator documentTemplateSectionFormValidator;

  public LmsDocumentTemplateSectionFormValidator(DocumentTemplateSectionFormValidator documentTemplateSectionFormValidator) {
    this.documentTemplateSectionFormValidator = documentTemplateSectionFormValidator;
  }

  void validate(LmsDocumentTemplateSectionForm form, Errors errors, DocumentTemplateDto documentTemplateDto) {
    documentTemplateSectionFormValidator.validate(form, documentTemplateDto, errors);
  }
}

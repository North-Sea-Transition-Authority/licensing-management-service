package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates;

import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionForm;
import uk.co.fivium.formlibrary.input.StringInput;

public class LmsDocumentTemplateSectionForm extends DocumentTemplateSectionForm {

  private StringInput comments = new StringInput("comments", "comments");

  public StringInput getComments() {
    return comments;
  }

  public void setComments(StringInput comments) {
    this.comments = comments;
  }

  public static LmsDocumentTemplateSectionForm setDocumentTemplateProperties(DocumentTemplateSectionDto sectionDto) {
    var form = new LmsDocumentTemplateSectionForm();
    form.setTitle(sectionDto.title());
    form.setContent(sectionDto.content());
    form.setConditionMnemonic(sectionDto.conditionMnemonic());
    form.setHasPageBreakBefore(sectionDto.hasPageBreakBefore());
    form.setNumbered(sectionDto.numbered());
    return form;
  }

  public static LmsDocumentTemplateSectionForm from(DocumentTemplateSectionDto documentTemplateSectionDto) {
    throw new UnsupportedOperationException("Cannot instantiate form from this static method.");
  }
}

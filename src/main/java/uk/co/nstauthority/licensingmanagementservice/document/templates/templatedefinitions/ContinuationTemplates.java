package uk.co.nstauthority.licensingmanagementservice.document.templates.templatedefinitions;

import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplate;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateProvider;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateType;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.sectionconditions.ContinuationCondition;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@Component
public class ContinuationTemplates implements DocumentTemplateProvider {

  private static final String PETROLEUM_ACT_CONTINUATION = "PETROLEUM ACT 1998 - CONTINUATION";

  @Override
  public DocumentTemplate getTemplate() {
    return DocumentTemplate.newBuilder()
            .withTemplate(DocumentTemplateType.CONTINUATION_LETTER)
              .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
              .withApplicationType(ApplicationType.CONTINUATION_APPLICATION)
              .withDisplayOrder(10)
              .withSection(PETROLEUM_ACT_CONTINUATION)
                .withContentFreemarker("lms/document/sectioncontent/continuation/continuationLetter.ftl")
                .withDisplayOrder(10)
                .withConditionMnemonic(ContinuationCondition.MNEMONIC)
                .completeSection()
            .build();
  }
}

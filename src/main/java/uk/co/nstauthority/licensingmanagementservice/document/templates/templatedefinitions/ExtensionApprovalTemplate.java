package uk.co.nstauthority.licensingmanagementservice.document.templates.templatedefinitions;

import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplate;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateProvider;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@Component
public class ExtensionApprovalTemplate implements DocumentTemplateProvider {

  private static final String EXTENSION_APPROVAL = "Extension Approval";

  @Override
  public DocumentTemplate getTemplate() {
    return DocumentTemplate.newBuilder()
            .withTemplate(DocumentTemplateType.EXTENSION_APPROVAL_LETTER)
              .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
              .withApplicationType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION)
              .withDisplayOrder(20)
              .withSection(EXTENSION_APPROVAL)
                .withContentFreemarker("lms/document/sectioncontent/eaa/extensionApprovalLetter.ftl")
                .withDisplayOrder(10)
                .completeSection()
            .build();
  }
}
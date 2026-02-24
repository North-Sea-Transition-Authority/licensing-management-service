package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentLinkingService;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@ExtendWith(MockitoExtension.class)
class CompanyNameMailMergeFieldTest {

  @Mock
  private DocumentLinkingService linkingService;

  @InjectMocks
  private CompanyNameMailMergeField mailMergeField;

  @Test
  void getMnemonic() {
    assertThat(mailMergeField.getMnemonic()).isEqualTo(CompanyNameMailMergeField.MNEMONIC);
  }

  @Test
  void getDescription() {
    assertThat(mailMergeField.getDescription()).isEqualTo(CompanyNameMailMergeField.DESCRIPTION);
  }

  @Test
  void isApplicable() {
    var documentTemplateDto = DocumentTemplateDtoTestUtil
        .newBuilder()
        .build();

    assertThat(mailMergeField.isApplicable(documentTemplateDto)).isTrue();
  }

  @Test
  void resolve() {
    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.CONTINUATION_APPLICATION.name())
        .withItemReference(UUID.randomUUID().toString())
        .build();

    var companyName = "company name";

    when(linkingService.getApplicationCompanyNameFromDto(documentInstanceDto)).thenReturn("company name");

    assertThat(mailMergeField.resolve(documentInstanceDto)).isEqualTo(DocumentMailMergeFieldResolveResult.success(companyName));
  }
}
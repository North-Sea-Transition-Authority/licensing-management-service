package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.DigitalSignatureMailMergeField.DESCRIPTION;
import static uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.DigitalSignatureMailMergeField.MNEMONIC;
import static uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.DigitalSignatureMailMergeField.SIGNATURE_PLACEHOLDER_TEXT;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;

@ExtendWith(MockitoExtension.class)
class DigitalSignatureMailMergeFieldTest {

  @InjectMocks
  private DigitalSignatureMailMergeField digitalSignatureMailMergeField;

  @Test
  void getMnemonic() {
    assertThat(digitalSignatureMailMergeField.getMnemonic()).isEqualTo(MNEMONIC);
  }

  @Test
  void getDescription() {
    assertThat(digitalSignatureMailMergeField.getDescription()).isEqualTo(DESCRIPTION);
  }

  @Test
  void isApplicable() {
    var documentTemplateDto = DocumentTemplateDtoTestUtil
        .newBuilder().build();
    assertThat(digitalSignatureMailMergeField.isApplicable(documentTemplateDto)).isTrue();
  }

  @Test
  void resolve() {
    var documentInstance = DocumentInstanceDtoTestUtil
        .newBuilder().build();
    assertThat(digitalSignatureMailMergeField.resolve(documentInstance))
        .isEqualTo(DocumentMailMergeFieldResolveResult.success(SIGNATURE_PLACEHOLDER_TEXT));
  }
}
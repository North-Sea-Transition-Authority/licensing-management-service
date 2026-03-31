package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.nstauthority.licensingmanagementservice.branding.CustomerConfigurationProperties;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;

@ExtendWith(MockitoExtension.class)
class RegulatorMnemonicMailMergeFieldTest {

  @Mock
  private CustomerConfigurationProperties customerConfigurationProperties;

  @InjectMocks
  private RegulatorMnemonicMailMergeField mailMergeField;

  @Test
  void getMnemonic() {
    assertThat(mailMergeField.getMnemonic()).isEqualTo(RegulatorMnemonicMailMergeField.MNEMONIC);
  }

  @Test
  void getDescription() {
    assertThat(mailMergeField.getDescription()).isEqualTo(RegulatorMnemonicMailMergeField.DESCRIPTION);
  }

  @Test
  void isApplicable() {
    var documentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().build();

    assertThat(mailMergeField.isApplicable(documentTemplateDto)).isTrue();
  }

  @Test
  void resolve() {
    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder().build();

    var regulatorMnemonic = "NSTA";

    when(customerConfigurationProperties.mnemonic()).thenReturn(regulatorMnemonic);

    assertThat(mailMergeField.resolve(documentInstanceDto))
        .isEqualTo(DocumentMailMergeFieldResolveResult.successNoEsc(regulatorMnemonic));
  }
}
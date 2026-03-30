package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.SignatoryNameMailMergeField.DESCRIPTION;
import static uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.SignatoryNameMailMergeField.MNEMONIC;
import static uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.SignatoryNameMailMergeField.USER_LOOKUP_PURPOSE;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentInstanceDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class SignatoryNameMailMergeFieldTest {

  @Mock
  private UserDetailService userDetailService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @InjectMocks
  private SignatoryNameMailMergeField signatoryNameMailMergeField;

  @Test
  void getMnemonic() {
    assertThat(signatoryNameMailMergeField.getMnemonic()).isEqualTo(MNEMONIC);
  }

  @Test
  void getDescription() {
    assertThat(signatoryNameMailMergeField.getDescription()).isEqualTo(DESCRIPTION);
  }

  @Test
  void isApplicable() {
    var documentTemplateDto = DocumentTemplateDtoTestUtil
        .newBuilder().build();
    assertThat(signatoryNameMailMergeField.isApplicable(documentTemplateDto)).isTrue();
  }

  @Test
  void resolve_success() {
    var documentInstance = DocumentInstanceDtoTestUtil
        .newBuilder().build();
    var userDetails = EnergyPortalUserTestUtil
        .newBuilder()
        .withForename("Howard")
        .withSurname("Moon")
        .build();
    var user = ServiceUserDetailTestUtil
        .newBuilder().build();
    var energyPortalUser = EnergyPortalUserJson.from(userDetails);
    when(userDetailService.getUserDetail()).thenReturn(user);
    when(energyPortalUserService.findByWuaId(WebUserAccountId.from(user.wuaId()), USER_LOOKUP_PURPOSE))
        .thenReturn(Optional.of(energyPortalUser));

    assertThat(signatoryNameMailMergeField.resolve(documentInstance))
        .isEqualTo(DocumentMailMergeFieldResolveResult.success(energyPortalUser.displayName()));
  }

  @Test
  void resolve_error() {
    var documentInstance = DocumentInstanceDtoTestUtil.newBuilder().build();
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    when(userDetailService.getUserDetail()).thenReturn(user);
    when(energyPortalUserService.findByWuaId(WebUserAccountId.from(user.wuaId()), USER_LOOKUP_PURPOSE))
        .thenReturn(Optional.empty());

    assertThat(signatoryNameMailMergeField.resolve(documentInstance))
        .isEqualTo(DocumentMailMergeFieldResolveResult.error("No mail merge value found for ((%s))".formatted(MNEMONIC)));
  }
}
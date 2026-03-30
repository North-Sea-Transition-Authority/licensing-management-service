package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeField;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;

@Order(DocumentMailMergeFieldDisplayOrders.SIGNATORY_NAME)
@Component
public class SignatoryNameMailMergeField implements DocumentMailMergeField {

  static final String MNEMONIC = "SIGNATORY_NAME";
  static final String DESCRIPTION = "The name of the person signing the document";
  static final String USER_LOOKUP_PURPOSE = "Lookup user details for mail merge";

  private final UserDetailService userDetailService;
  private final EnergyPortalUserService energyPortalUserService;

  @Autowired
  public SignatoryNameMailMergeField(UserDetailService userDetailService, EnergyPortalUserService energyPortalUserService) {
    this.userDetailService = userDetailService;
    this.energyPortalUserService = energyPortalUserService;
  }

  @Override
  public String getMnemonic() {
    return MNEMONIC;
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public boolean isApplicable(DocumentTemplateDto documentTemplateDto) {
    return true;
  }

  @Override
  public DocumentMailMergeFieldResolveResult resolve(DocumentInstanceDto documentInstanceDto) {
    var currentUserWuaId = WebUserAccountId.from(userDetailService.getUserDetail().wuaId());
    var optionalUserJson = energyPortalUserService.findByWuaId(currentUserWuaId, USER_LOOKUP_PURPOSE);

    return optionalUserJson
        .map(energyPortalUserJson -> DocumentMailMergeFieldResolveResult.success(energyPortalUserJson.displayName()))
        .orElseGet(() -> DocumentMailMergeFieldResolveResult.error("No mail merge value found for ((%s))".formatted(MNEMONIC)));
  }
}

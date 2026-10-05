package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Service
public class CorrectionDetailsViewService {

  private final EnergyPortalUserService energyPortalUserService;

  public CorrectionDetailsViewService(EnergyPortalUserService energyPortalUserService) {
    this.energyPortalUserService = energyPortalUserService;
  }

  public CorrectionDetailsView getDetailsView(LicenceCorrection licenceCorrection) {
    var allocatedToUserDetail = energyPortalUserService.getByWuaId(
        WebUserAccountId.from(licenceCorrection.getAllocatedToWuaId()),
        "Get correction allocated to user details"
    );

    return new CorrectionDetailsView(
        licenceCorrection.getCorrectionReference(),
        licenceCorrection.getReason(),
        allocatedToUserDetail.displayName(),
        licenceCorrection.getStatus().getDisplayName(),
        licenceCorrection.getLicence().getLicenceReference(),
        DateUtil.formatLongDate(licenceCorrection.getCreatedInstant())
    );
  }
}
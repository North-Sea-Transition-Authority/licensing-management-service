package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionService;

@Controller
@RequestMapping("licences/{licenceId}/overview")
public class LicenceOverviewController {
  private final LicenceActionService licenceActionService;

  public LicenceOverviewController(LicenceActionService licenceActionService) {
    this.licenceActionService = licenceActionService;
  }

  @GetMapping
  public ModelAndView renderLicenceOverview(
      @PathVariable Integer licenceId,
      Licence licence,
      ServiceUserDetail user
  ) {
    return new ModelAndView("lms/licence/licenceOverview")
        .addObject("licenceReference", licence.getLicenceReference())
        .addObject("caption", licence.getType().getDisplayName())
        .addObject("licenceActions", licenceActionService.getAvailableUserActionItems(licence, user));
  }
}

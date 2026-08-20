package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceScheduleTabController;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTab;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTabContext;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;

@Component
public class LicenceScheduleTab implements LicenceTab {

  @Override
  public String displayName() {
    return "Schedule";
  }

  @Override
  public int displayOrder() {
    return 2;
  }

  @Override
  public ReleaseFeature getReleaseFeature() {
    return ReleaseFeature.VIEW_LICENCE_SCHEDULE;
  }

  @Override
  public String url(LicenceTabContext context) {
    return ReverseRouter.route(on(LicenceScheduleTabController.class)
        .renderLicenceOverview(context.licence().getId(), null, null, null));
  }
}

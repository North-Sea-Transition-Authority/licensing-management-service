package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTab;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTabContext;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;

@Component
public class LicenceTimelinePositionTab implements LicenceTab {

  @Override
  public String displayName() {
    return "Timeline";
  }

  @Override
  public int displayOrder() {
    return 1;
  }

  @Override
  public ReleaseFeature getReleaseFeature() {
    return ReleaseFeature.VIEW_LICENCE_TIMELINE;
  }

  @Override
  public String url(LicenceTabContext context) {
    return ReverseRouter.route(on(LicencePositionController.class).renderLicencePositionTimeline(context.licence(), null));
  }

}

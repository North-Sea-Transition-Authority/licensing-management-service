package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTab;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTabContext;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Component
@Profile("enable-lms2")
public class LicenceTimelinePositionTab implements LicenceTab {

  @Override
  public String displayName() {
    return "Timeline";
  }

  @Override
  public String url(LicenceTabContext context) {
    return ReverseRouter.route(on(LicencePositionController.class).renderLicencePositionTimeline(context.licence(), null));
  }

}

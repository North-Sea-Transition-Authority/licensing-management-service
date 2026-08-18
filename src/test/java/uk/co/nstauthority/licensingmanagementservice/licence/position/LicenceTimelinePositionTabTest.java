package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTabContext;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;

class LicenceTimelinePositionTabTest {

  private final LicenceTimelinePositionTab licenceTimelinePositionTab = new LicenceTimelinePositionTab();

  @Test
  void displayName() {
    assertThat(licenceTimelinePositionTab.displayName()).isEqualTo("Timeline");
  }

  @Test
  void displayOrder() {
    assertThat(licenceTimelinePositionTab.displayOrder()).isEqualTo(1);
  }

  @Test
  void getReleaseFeature() {
    assertThat(licenceTimelinePositionTab.getReleaseFeature()).isEqualTo(ReleaseFeature.VIEW_LICENCE_TIMELINE);
  }

  @Test
  void anchor() {
    assertThat(licenceTimelinePositionTab.anchor()).isEqualTo("timeline");
  }

  @Test
  void url() {
    var licence = LicenceTestUtil.builder().withId(1).build();

    assertThat(licenceTimelinePositionTab.url(new LicenceTabContext(licence)))
        .isEqualTo(ReverseRouter.route(on(LicencePositionController.class)
            .renderLicencePositionTimeline(licence, null)));
  }
}

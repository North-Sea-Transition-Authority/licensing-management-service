package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTabContext;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;

class LicenceScheduleTabTest {

  private final LicenceScheduleTab licenceScheduleTab = new LicenceScheduleTab();

  @Test
  void displayName() {
    assertThat(licenceScheduleTab.displayName()).isEqualTo("Schedule");
  }

  @Test
  void displayOrder() {
    assertThat(licenceScheduleTab.displayOrder()).isEqualTo(2);
  }

  @Test
  void getReleaseFeature() {
    assertThat(licenceScheduleTab.getReleaseFeature()).isEqualTo(ReleaseFeature.VIEW_LICENCE_SCHEDULE);
  }

  @Test
  void anchor() {
    assertThat(licenceScheduleTab.anchor()).isEqualTo("schedule");
  }

  @Test
  void url() {
    var licence = LicenceTestUtil.builder().withId(1).build();

    assertThat(licenceScheduleTab.url(new LicenceTabContext(licence)))
        .isEqualTo(ReverseRouter.route(on(LicenceOverviewController.class)
            .renderLicenceOverview(licence.getId(), null, null, null)));
  }
}

package uk.co.nstauthority.licensingmanagementservice.fds.tab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTab;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTabContext;

class FdsBackendTabTest {

  @Test
  void from_assertLabelAnchorAndUrlTakenFromLicenceTab() {
    var licence = LicenceTestUtil.builder().withId(1).build();
    var context = new LicenceTabContext(licence);

    assertThat(FdsBackendTab.from(new TestLicenceTab("Overview"), context))
        .isEqualTo(new FdsBackendTab("Overview", "overview", "/tab/1"));
  }

  @Test
  void from_whenMultipleWordDisplayName_assertAnchorDerivedFromDisplayName() {
    var context = new LicenceTabContext(LicenceTestUtil.builder().withId(2).build());

    assertThat(FdsBackendTab.from(new TestLicenceTab("Licence position"), context))
        .isEqualTo(new FdsBackendTab("Licence position", "licence-position", "/tab/2"));
  }

  private record TestLicenceTab(String displayName) implements LicenceTab {

    @Override
    public String url(LicenceTabContext context) {
      return "/tab/%d".formatted(context.licence().getId());
    }
  }
}

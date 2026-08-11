package uk.co.nstauthority.licensingmanagementservice.licence.tab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;

class LicenceTabTest {

  @Test
  void anchor_whenSingleWordDisplayName_assertLowerCased() {
    assertThat(new TestLicenceTab("Overview").anchor()).isEqualTo("overview");
  }

  @Test
  void anchor_whenMultipleWordDisplayName_assertSpacesReplacedWithHyphens() {
    assertThat(new TestLicenceTab("Licence position and schedule").anchor())
        .isEqualTo("licence-position-and-schedule");
  }

  @Test
  void actions_whenNotOverridden_assertNoActions() {
    var licence = LicenceTestUtil.builder().withId(1).build();
    var user = ServiceUserDetailTestUtil.newBuilder().build();

    assertThat(new TestLicenceTab("Overview").actions(licence, user)).isEmpty();
  }

  private record TestLicenceTab(String displayName) implements LicenceTab {

    @Override
    public String url(LicenceTabContext context) {
      return "/tab";
    }
  }
}

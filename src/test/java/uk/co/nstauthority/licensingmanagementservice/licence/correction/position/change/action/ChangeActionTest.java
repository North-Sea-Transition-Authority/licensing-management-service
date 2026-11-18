package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.fds.button.Button;

class ChangeActionTest {

  private static final String CHANGE_NAME = "licensee change";

  @Test
  void pageTitle() {
    var result = ChangeAction.UNDO.pageTitle(CHANGE_NAME);

    assertThat(result).isEqualTo("Are you sure you want to undo this licensee change?");
  }

  @Test
  void primaryButton_whenRemoving_thenWarningButton() {
    var result = ChangeAction.REMOVE.primaryButton(CHANGE_NAME);

    assertThat(result).isEqualTo(Button.warningButton("Remove licensee change"));
  }

  @Test
  void primaryButton_whenUndoing_thenWarningButton() {
    var result = ChangeAction.UNDO.primaryButton(CHANGE_NAME);

    assertThat(result).isEqualTo(Button.warningButton("Undo licensee change"));
  }

  @Test
  void primaryButton_whenReinstating_thenPrimaryButton() {
    var result = ChangeAction.REINSTATE.primaryButton(CHANGE_NAME);

    assertThat(result).isEqualTo(Button.primaryButton("Reinstate licensee change"));
  }

  @Test
  void successBannerHeading() {
    var result = ChangeAction.REINSTATE.successBannerHeading(CHANGE_NAME);

    assertThat(result).isEqualTo("Licensee change reinstated");
  }
}

package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import jakarta.servlet.http.HttpSession;
import java.util.function.Consumer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mockups")
class UploadDspMockupJourneyStore {

  // Bump the suffix after an incompatible change to the journey: an unreadable saved value can't be removed or replaced
  private static final String JOURNEY = "uploadDspMockupJourney.v5";

  private final HttpSession session;

  UploadDspMockupJourneyStore(HttpSession session) {
    this.session = session;
  }

  UploadDspMockupJourney journey() {
    return session.getAttribute(JOURNEY) instanceof UploadDspMockupJourney existing
        ? existing
        : new UploadDspMockupJourney();
  }

  // Spring Session JDBC only persists an attribute when it is set, so always save after changing the journey
  void update(Consumer<UploadDspMockupJourney> change) {
    var journey = journey();
    change.accept(journey);
    session.setAttribute(JOURNEY, journey);
  }

  void reset() {
    session.removeAttribute(JOURNEY);
  }
}

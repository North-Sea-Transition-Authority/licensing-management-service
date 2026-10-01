package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

class LicenceTimelineFilterSessionTest {

  @Test
  void getChangeTypes_whenNoFilterForLicence_thenEmpty() {
    var session = new LicenceTimelineFilterSession();

    var result = session.getChangeTypes("REF-1");

    assertThat(result).isEmpty();
  }

  @Test
  void update_whenChangeTypesGiven_thenOnlyHeldAgainstThatLicence() {
    var session = new LicenceTimelineFilterSession();

    session.update("REF-1", Set.of(LicenceOperation.SET_EQUITY));

    assertThat(session.getChangeTypes("REF-1")).containsExactly(LicenceOperation.SET_EQUITY);
    assertThat(session.getChangeTypes("REF-2")).isEmpty();
  }

  @Test
  void update_whenNoChangeTypesGiven_thenLicenceFilterIsRemoved() {
    var session = new LicenceTimelineFilterSession();
    session.update("REF-1", Set.of(LicenceOperation.SET_EQUITY));

    session.update("REF-1", Set.of());

    assertThat(session.isEmpty()).isTrue();
  }

  @Test
  void clear_whenOtherLicencesFiltered_thenOnlyThatLicenceFilterIsRemoved() {
    var session = new LicenceTimelineFilterSession();
    session.update("REF-1", Set.of(LicenceOperation.SET_EQUITY));
    session.update("REF-2", Set.of(LicenceOperation.LICENSEE));

    session.clear("REF-1");

    assertThat(session.getChangeTypes("REF-1")).isEmpty();
    assertThat(session.getChangeTypes("REF-2")).containsExactly(LicenceOperation.LICENSEE);
    assertThat(session.isEmpty()).isFalse();
  }
}

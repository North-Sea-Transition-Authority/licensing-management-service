package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

class LicenceTimelineFilterSessionTest {

  private static final LicenceTimelineFilter SET_EQUITY_FILTER =
      new LicenceTimelineFilter(Set.of(LicenceOperation.SET_EQUITY), Set.of());

  private static final LicenceTimelineFilter ORGANISATION_FILTER = new LicenceTimelineFilter(Set.of(), Set.of(1));

  @Test
  void getFilter_whenNoFilterForLicence_thenEmpty() {
    var session = new LicenceTimelineFilterSession();

    var result = session.getFilter("REF-1");

    assertThat(result).isEqualTo(LicenceTimelineFilter.empty());
  }

  @Test
  void update_whenFilterGiven_thenOnlyHeldAgainstThatLicence() {
    var session = new LicenceTimelineFilterSession();

    session.update("REF-1", SET_EQUITY_FILTER);

    assertThat(session.getFilter("REF-1")).isEqualTo(SET_EQUITY_FILTER);
    assertThat(session.getFilter("REF-2")).isEqualTo(LicenceTimelineFilter.empty());
  }

  @Test
  void update_whenEmptyFilterGiven_thenLicenceFilterIsRemoved() {
    var session = new LicenceTimelineFilterSession();
    session.update("REF-1", SET_EQUITY_FILTER);

    session.update("REF-1", LicenceTimelineFilter.empty());

    assertThat(session.isEmpty()).isTrue();
  }

  @Test
  void clear_whenOtherLicencesFiltered_thenOnlyThatLicenceFilterIsRemoved() {
    var session = new LicenceTimelineFilterSession();
    session.update("REF-1", SET_EQUITY_FILTER);
    session.update("REF-2", ORGANISATION_FILTER);

    session.clear("REF-1");

    assertThat(session.getFilter("REF-1")).isEqualTo(LicenceTimelineFilter.empty());
    assertThat(session.getFilter("REF-2")).isEqualTo(ORGANISATION_FILTER);
    assertThat(session.isEmpty()).isFalse();
  }
}

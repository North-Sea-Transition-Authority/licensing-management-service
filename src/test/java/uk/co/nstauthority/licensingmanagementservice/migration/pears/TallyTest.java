package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TallyTest {

  private final Tally tally = new Tally("Heading:");

  @Test
  void appendTo_whenLicencesAreAddedOutOfSweepOrder_thenTheyAreListedInSweepOrder() {
    tally.add("differs", "P10");
    tally.add("differs", "PEDL1");
    tally.add("differs", "P2");
    tally.add("differs", "AL2");

    var out = new StringBuilder();
    tally.appendTo(out);

    assertThat(out.toString()).contains("      AL2, P2, P10, PEDL1%n".formatted());
  }

  @Test
  void appendTo_whenALaterLicenceInTheSweepOffersAnExampleFirst_thenTheEarlierLicencesExampleIsShown() {
    tally.add("differs", "P10");
    tally.example("differs", "P10", "detail of P10");
    tally.add("differs", "P2");
    tally.example("differs", "P2", "detail of P2");

    var out = new StringBuilder();
    tally.appendTo(out);

    assertThat(out.toString())
        .contains("e.g. P2 -- detail of P2")
        .doesNotContain("detail of P10");
  }

  @Test
  void appendTo_whenOneLicenceOffersSeveralExamples_thenItsFirstIsShown() {
    tally.add("differs", "P1");
    tally.example("differs", "P1", "first detail");
    tally.example("differs", "P1", "second detail");

    var out = new StringBuilder();
    tally.appendTo(out);

    assertThat(out.toString())
        .contains("e.g. P1 -- first detail")
        .doesNotContain("second detail");
  }
}

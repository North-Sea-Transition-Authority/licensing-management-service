package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds a {@link PearsScheduleEvent} for a test, setting only what the test needs. Each event gets its own index, as
 * events in one PEARS version do.
 */
final class PearsEventFixture {

  private static final AtomicInteger NEXT_INDEX = new AtomicInteger(1);

  private final String eventMnem;
  private final LocalDate date;
  private String termNo;
  private String phase;
  private String years;
  private String months;
  private String days;
  private String recurring;
  private String rate;
  private String percentage;
  private String comments;

  private PearsEventFixture(String eventMnem, LocalDate date) {
    this.eventMnem = eventMnem;
    this.date = date;
  }

  static PearsEventFixture event(String eventMnem, LocalDate date) {
    return new PearsEventFixture(eventMnem, date);
  }

  static PearsEventFixture term(String termNo, LocalDate date) {
    return event("T", date).withTermNo(termNo);
  }

  static PearsEventFixture phase(String phase, LocalDate date) {
    return event("P", date).withPhase(phase);
  }

  PearsEventFixture withTermNo(String termNo) {
    this.termNo = termNo;
    return this;
  }

  PearsEventFixture withPhase(String phase) {
    this.phase = phase;
    return this;
  }

  PearsEventFixture withOffset(int years, int months, int days) {
    this.years = String.valueOf(years);
    this.months = String.valueOf(months);
    this.days = String.valueOf(days);
    return this;
  }

  PearsEventFixture withYearlyRate(String rate) {
    this.recurring = "YEARLY";
    this.rate = rate;
    return this;
  }

  PearsEventFixture withPercentage(String percentage) {
    this.percentage = percentage;
    return this;
  }

  PearsEventFixture withComments(String comments) {
    this.comments = comments;
    return this;
  }

  PearsScheduleEvent build() {
    return new PearsScheduleEvent(NEXT_INDEX.getAndIncrement(), eventMnem, termNo, phase, years, months, days, date,
        recurring, null, null, rate, percentage, comments, null, null, null);
  }
}

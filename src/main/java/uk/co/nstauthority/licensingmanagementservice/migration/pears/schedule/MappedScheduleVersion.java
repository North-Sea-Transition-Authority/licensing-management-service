package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

/**
 * One PEARS schedule version, mapped to what its LMS schedule detail will hold.
 *
 * @param plsdId             the PEARS version ({@code ped_licence_schedule_details.id})
 * @param status             {@code ACTIVE} for the current version, or the latest when none is current; otherwise
 *                           {@code REPLACED}
 * @param createdInstant     when the version was saved in PEARS
 * @param startDate          the schedule start date
 * @param startDateBorrowed  whether the version has no start date in PEARS, so {@code startDate} is another version's
 * @param datesFromOffsets   whether the event dates were worked out from their offsets rather than taken from PEARS:
 *                           when the start date is borrowed, or the dates PEARS stored are on or before the start date
 *                           (from its 1900 base). The comparison report shows these versions' dates as not from PEARS
 *                           rather than as differences
 * @param terms              the terms, in term order
 * @param phases             the initial term's phases, in phase order; empty when it has none
 * @param expiry             the expiry, or null when PEARS has no expiry event
 * @param rates              the rates, in date order
 * @param otherEvents        the other events ({@code MR}, {@code BM}, {@code DD} and so on), in the order PEARS holds
 *                           them
 */
public record MappedScheduleVersion(
    long plsdId,
    LicenceScheduleDetailStatus status,
    Instant createdInstant,
    LocalDate startDate,
    boolean startDateBorrowed,
    boolean datesFromOffsets,
    List<MappedTerm> terms,
    List<MappedPhase> phases,
    @Nullable MappedExpiry expiry,
    List<MappedRate> rates,
    List<MappedOtherEvent> otherEvents
) {

  public MappedScheduleVersion {
    terms = List.copyOf(terms);
    phases = List.copyOf(phases);
    rates = List.copyOf(rates);
    otherEvents = List.copyOf(otherEvents);
  }

  /**
   * The last term, whose boundary is where the schedule ends.
   */
  public MappedTerm finalTerm() {
    return terms.getLast();
  }
}

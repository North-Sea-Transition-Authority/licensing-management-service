package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;

/**
 * An LMS other schedule event of a mapped schedule version, from a PEARS {@code MR}, {@code BM}, {@code DD},
 * {@code 2M}, {@code 2N} or user-defined event. It is dated relative to the start of the term it falls in.
 *
 * @param eventKey                 see {@link MappedTerm#eventKey}
 * @param otherCategoryName        for {@code OTHER_ACTIVITY}, the PEARS event type's short title; otherwise null
 * @param defaultDescription       whether PEARS had no comments, so {@code description} is a default
 * @param relinquishmentPercentage for a mandatory relinquishment, its percentage; null when PEARS has none or one that
 *                                 can't be used
 * @param date                     the PEARS date, which LMS lands on from {@code termNo} and {@code relativeDuration}
 * @param termNo                   PEARS's number for the term it is anchored to
 */
public record MappedOtherEvent(
    int eventKey,
    Category category,
    @Nullable String otherCategoryName,
    String description,
    boolean defaultDescription,
    @Nullable BigDecimal relinquishmentPercentage,
    LocalDate date,
    int termNo,
    ThreeFieldDuration relativeDuration,
    PearsScheduleEvent source
) {

  /**
   * The LMS category. {@code BESPOKE_MILESTONE} is not in LMS yet, so the write step turns these into
   * {@code OtherScheduleEventCategory} values.
   */
  public enum Category {
    MANDATORY_RELINQUISHMENT,
    BESPOKE_MILESTONE,
    OTHER_ACTIVITY
  }
}

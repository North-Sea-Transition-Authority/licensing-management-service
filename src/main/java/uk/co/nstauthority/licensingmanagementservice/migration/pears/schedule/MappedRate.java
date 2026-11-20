package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateRelativeDateOption;

/**
 * An LMS rate of a mapped schedule version, from a PEARS {@code R} event. A PEARS rate becomes one LMS rate in each
 * term or phase it is in force in (plan, Open question 7, option A): on its own date in the first, then on its first
 * yearly anniversary in each later one. Like any LMS rate, it is in force until the next one starts.
 *
 * <p>How it is anchored follows LMS's own rules ({@code LicenceScheduleCalculationService}): a {@code TERM} or
 * {@code PHASE} rate starts and ends with its period, so it is used only when the rate starts on the period's start
 * and the next rate starts on the period's end. Every other rate is a {@code CUSTOM_PERIOD} rate, which runs until the
 * next rate starts.
 *
 * @param eventKey           see {@link MappedTerm#eventKey}. Each LMS rate from one PEARS rate has its own key
 * @param startDate          the day the rate starts, as LMS works it out from the anchor
 * @param rentalRate         the rate, as PEARS holds it, rounded to pence
 * @param termNo             PEARS's number for the term it is anchored to (or whose phase it is anchored to)
 * @param phaseType          the initial term's phase it is anchored to, or null when it is anchored to the term
 * @param definitionOption   {@code TERM}, {@code PHASE} or {@code CUSTOM_PERIOD}
 * @param relativeDateOption for a {@code CUSTOM_PERIOD} rate, whether it starts on the period's start or relative to
 *                           it; otherwise null
 * @param relativeDuration   for a rate relative to the period's start, how long after it; otherwise null
 * @param comment            the {@code R} event's comments, on the first LMS rate from it only; otherwise null
 * @param source             the {@code R} event
 */
public record MappedRate(
    int eventKey,
    LocalDate startDate,
    BigDecimal rentalRate,
    int termNo,
    @Nullable PhaseType phaseType,
    RateDefinitionOption definitionOption,
    @Nullable RateRelativeDateOption relativeDateOption,
    @Nullable ThreeFieldDuration relativeDuration,
    @Nullable MappedComment comment,
    PearsScheduleEvent source
) {
}

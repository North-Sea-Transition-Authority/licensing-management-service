package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;

/**
 * One schedule event type PEARS knows: a row of {@code PED_TEMPLATE_EVENT_TYPES}.
 *
 * @param mnem       the code events carry in {@code EVENT_MNEM}, for example {@code BM}
 * @param shortTitle the name PEARS shows for the type, for example {@code Drill-or-drop}
 * @param displaySeq the type's place on the schedule screen; below zero for the fixed columns ({@code P}, {@code T},
 *                   {@code R}, {@code MR})
 */
public record PearsScheduleEventType(String mnem, @Nullable String shortTitle, @Nullable Integer displaySeq) {
}

package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;

/**
 * One note of a work programme's current version, as PEARS holds it.
 *
 * @param position  the note's position in the programme's {@code NOTES_LIST}, counting from 1
 * @param type      {@code GENERAL}, or the new status of a status-change note
 * @param datetime  when the note was written; null when PEARS holds none, or one that is not a date and time
 * @param createdBy the author's full name as PEARS stored it, or {@code Migration Process} for the 2011 bulk job
 */
public record PearsNote(
    int position,
    @Nullable String type,
    @Nullable String activityId,
    @Nullable String activityTitle,
    @Nullable String newActivityStatus,
    @Nullable LocalDateTime datetime,
    @Nullable String createdBy,
    @Nullable String description
) {
}

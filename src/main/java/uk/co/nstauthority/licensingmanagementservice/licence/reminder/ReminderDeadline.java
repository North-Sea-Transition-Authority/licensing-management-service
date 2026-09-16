package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.time.LocalDate;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;

public record ReminderDeadline(
    ScheduleEvent scheduleEvent,
    UUID originalEventId,
    Licence licence,
    LocalDate deadlineDate,
    String displayName,
    ReminderType reminderType
) {
}

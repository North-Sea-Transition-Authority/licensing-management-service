package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import java.time.LocalDate;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;

public record EventReminderDeadline(
    ScheduleEvent scheduleEvent,
    UUID originalEventId,
    Licence licence,
    LocalDate deadlineDate,
    String displayName,
    ReminderType reminderType
) {
}

package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import java.util.List;

public interface EventReminderDeadlineSource {

  String DISPLAY_NAME_FORMAT = "%s: %s";

  List<EventReminderDeadline> getDeadlinesDueReminder();
}

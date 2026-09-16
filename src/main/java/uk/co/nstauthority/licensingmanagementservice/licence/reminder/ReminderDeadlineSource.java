package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.util.List;

public interface ReminderDeadlineSource {

  String DISPLAY_NAME_FORMAT = "%s: %s";

  List<ReminderDeadline> getDeadlinesDueReminder();
}

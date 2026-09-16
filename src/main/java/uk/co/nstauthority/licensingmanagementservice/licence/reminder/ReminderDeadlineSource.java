package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.util.List;

public interface ReminderDeadlineSource {

  List<ReminderDeadline> getDeadlinesDueReminder(NoticePeriod noticePeriod);
}

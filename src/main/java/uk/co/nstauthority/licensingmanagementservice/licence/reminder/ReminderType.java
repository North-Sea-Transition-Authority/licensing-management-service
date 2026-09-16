package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

public enum ReminderType {
  TERM_OR_PHASE_END(NoticePeriod.SIX_MONTHS),
  LICENCE_EXPIRY(NoticePeriod.SIX_MONTHS),
  WORK_PROGRAMME_ACTIVITY(NoticePeriod.SIX_MONTHS),
  OTHER_SCHEDULE_EVENT(NoticePeriod.SIX_MONTHS);

  private final NoticePeriod noticePeriod;

  ReminderType(NoticePeriod noticePeriod) {
    this.noticePeriod = noticePeriod;
  }

  public NoticePeriod getNoticePeriod() {
    return noticePeriod;
  }
}

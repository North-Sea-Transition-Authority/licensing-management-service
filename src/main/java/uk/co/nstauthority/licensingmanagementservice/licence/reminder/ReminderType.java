package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum ReminderType implements Displayable {
  TERM_OR_PHASE_END(NoticePeriod.SIX_MONTHS, "Term or phase end", 10),
  LICENCE_EXPIRY(NoticePeriod.SIX_MONTHS, "Licence expiry", 20),
  WORK_PROGRAMME_ACTIVITY(NoticePeriod.SIX_MONTHS, "Work programme activity", 30),
  OTHER_SCHEDULE_EVENT(NoticePeriod.SIX_MONTHS, "Other schedule event", 40);

  private final NoticePeriod noticePeriod;
  private final String displayName;
  private final int displayOrder;

  ReminderType(NoticePeriod noticePeriod, String displayName, int displayOrder) {
    this.noticePeriod = noticePeriod;
    this.displayName = displayName;
    this.displayOrder = displayOrder;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  public NoticePeriod getNoticePeriod() {
    return noticePeriod;
  }
}

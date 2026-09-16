package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.time.LocalDate;

public enum NoticePeriod {

  SIX_MONTHS(6);

  private final int months;

  NoticePeriod(int months) {
    this.months = months;
  }

  public int getMonths() {
    return months;
  }

  /**
   * The furthest ahead deadline that is close enough to remind on today. Deriving the window forwards from today keeps
   * a single definition of the window: the bound a deadline query is given is the same bound a deadline is tested
   * against, so month-length clamping cannot make the two disagree.
   *
   * <p>Working backwards from the deadline instead is what makes them disagree, because month arithmetic clamps to the
   * length of the month it lands in. For a deadline of 31 August, {@code minusMonths(6)} clamps to 28 February, so the
   * deadline looks due on 28 February; but a query bounded by {@code 28 February plusMonths(6)} only reaches 28 August
   * and never returns it. Six months before 31 August does not exist, so we take the first day whose window reaches it
   * — 1 March — and the reminder still goes out with a little over six months' notice.</p>
   */
  public LocalDate getLatestDeadlineDate(LocalDate today) {
    return today.plusMonths(months);
  }

  public boolean isWithinNoticeWindow(LocalDate deadlineDate, LocalDate today) {
    return !deadlineDate.isBefore(today) && !deadlineDate.isAfter(getLatestDeadlineDate(today));
  }
}

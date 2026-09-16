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

  public LocalDate getNoticeDateFor(LocalDate deadlineDate) {
    return deadlineDate.minusMonths(months);
  }

  public boolean isDueBy(LocalDate deadlineDate, LocalDate today) {
    return !getNoticeDateFor(deadlineDate).isAfter(today);
  }
}

package uk.co.nstauthority.licensingmanagementservice.components.duration;

public class ThreeFieldDurationInput {

  private String fieldName;
  private String fieldDisplayText;
  private String days;
  private String months;
  private String years;

  public ThreeFieldDurationInput() {
  }

  public ThreeFieldDurationInput(String fieldName,
                                 String fieldDisplayText
  ) {
    this.fieldName = fieldName;
    this.fieldDisplayText = fieldDisplayText;
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getFieldDisplayText() {
    return fieldDisplayText;
  }

  public String getDays() {
    return days;
  }

  public void setDays(String days) {
    this.days = days;
  }

  public String getMonths() {
    return months;
  }

  public void setMonths(String months) {
    this.months = months;
  }

  public String getYears() {
    return years;
  }

  public void setYears(String years) {
    this.years = years;
  }

  public ThreeFieldDuration toThreeFieldDuration() {
    return new ThreeFieldDuration(Integer.parseInt(years), Integer.parseInt(months), Integer.parseInt(days));
  }

  public void setFromThreeFieldDuration(ThreeFieldDuration duration) {
    this.setDays(duration.days().toString());
    this.setMonths(duration.months().toString());
    this.setYears(duration.years().toString());
  }
}
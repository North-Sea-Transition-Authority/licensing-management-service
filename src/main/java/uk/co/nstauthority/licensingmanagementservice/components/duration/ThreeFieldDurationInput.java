package uk.co.nstauthority.licensingmanagementservice.components.duration;

public class ThreeFieldDurationInput {

  private final String fieldName;
  private final String fieldDisplayText;
  private String days;
  private String months;
  private String years;

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
    return new ThreeFieldDuration(Integer.parseInt(days), Integer.parseInt(months), Integer.parseInt(years));
  }
}

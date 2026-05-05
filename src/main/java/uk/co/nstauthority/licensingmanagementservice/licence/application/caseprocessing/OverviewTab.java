package uk.co.nstauthority.licensingmanagementservice.licence.application.caseprocessing;

public enum OverviewTab {

  OVERVIEW("Overview", "overview", "overview"),
  LETTER("Letter", "letter", "letter");

  private final String label;
  private final String value;
  private final String anchor;

  OverviewTab(String label, String value, String anchor) {
    this.label = label;
    this.value = value;
    this.anchor = anchor;
  }

  public String label() {
    return label;
  }

  public String value() {
    return value;
  }

  public String anchor() {
    return anchor;
  }

  public String getLabel() {
    return label;
  }

  public String getValue() {
    return value;
  }

  public String getAnchor() {
    return anchor;
  }

}
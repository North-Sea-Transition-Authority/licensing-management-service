package uk.co.nstauthority.licensingmanagementservice.summary;

public record SummaryCardAction(
    String text,
    String url,
    boolean post
) {

  public SummaryCardAction(
      String text,
      String url
  ) {
    this(text, url, false);
  }

  public static SummaryCardAction post(
      String text,
      String url
  ) {
    return new SummaryCardAction(text, url, true);
  }
}

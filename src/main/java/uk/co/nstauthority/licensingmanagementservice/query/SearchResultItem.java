package uk.co.nstauthority.licensingmanagementservice.query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

public record SearchResultItem(
    UUID id,
    String linkHeadingUrl,
    String linkHeadingText,
    String tagText,
    String tagClass,
    String captionText,
    List<SummaryDataView> dataItemRows
) {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private String linkHeadingUrl;
    private String linkHeadingText;
    private String tagText;
    private String tagClass;
    private String captionText;
    private final List<SummaryDataView> dataItemRows = new ArrayList<>();

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withLinkHeadingUrl(String linkHeadingUrl) {
      this.linkHeadingUrl = linkHeadingUrl;
      return this;
    }

    public Builder withLinkHeadingText(String linkHeadingText) {
      this.linkHeadingText = linkHeadingText;
      return this;
    }

    public Builder withTagText(String tagText) {
      this.tagText = tagText;
      return this;
    }

    public Builder withTagClass(String tagClass) {
      this.tagClass = tagClass;
      return this;
    }

    public Builder withCaptionText(String captionText) {
      this.captionText = captionText;
      return this;
    }

    public Builder withDataItemRow(SummaryDataView dateItemRow) {
      this.dataItemRows.add(dateItemRow);
      return this;
    }

    public SearchResultItem build() {
      return new SearchResultItem(
          id,
          linkHeadingUrl,
          linkHeadingText,
          tagText,
          tagClass,
          captionText,
          dataItemRows
      );
    }
  }
}

package uk.co.nstauthority.licensingmanagementservice.summary;

import static uk.co.nstauthority.licensingmanagementservice.summary.SummaryValueType.FILE_VALUE;
import static uk.co.nstauthority.licensingmanagementservice.summary.SummaryValueType.STRING_VALUE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public record SummaryDataView(
    List<SummaryKeyValue> keyValues
) {

  public static SummaryDataView newStringKeyValue(String key, String value) {
    return SummaryDataView.newBuilder()
        .addStringValue(key, value)
        .build();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private final List<SummaryKeyValue> keyValues = new ArrayList<>();

    private Builder() {
    }

    public Builder addStringValue(String key, Object value) {
      keyValues.add(
          new SummaryKeyValue(key,
              STRING_VALUE,
              SummaryUtil.formatAsList(value)
          )
      );
      return this;
    }

    public Builder addFileValue(String key, UploadedFileView value) {
      return addFileValue(key, List.of(value));
    }

    public Builder addFileValue(String key, Collection<UploadedFileView> value) {
      keyValues.add(
          new SummaryKeyValue(
              key,
              FILE_VALUE,
              value
          )
      );
      return this;
    }

    public SummaryDataView build() {
      return new SummaryDataView(Collections.unmodifiableList(keyValues));
    }
  }
}

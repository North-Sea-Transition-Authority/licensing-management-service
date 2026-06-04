package uk.co.nstauthority.licensingmanagementservice.mockups.contact;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record SortableTableRow(List<SortableTableValue> rowValues, List<SortableTableAction> rowActions) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final List<SortableTableValue> values = new ArrayList<>();
    private final List<SortableTableAction> actions = new ArrayList<>();

    public Builder withValue(String value) {
      values.add(new SortableTableValue(value));
      return this;
    }

    public Builder withValue(String value, Tag tag) {
      values.add(new SortableTableValue(value, List.of(tag)));
      return this;
    }

    public Builder withValue(SortableTableValue value) {
      values.add(value);
      return this;
    }

    public Builder withValues(String... vals) {
      Arrays.stream(vals).forEach(this::withValue);
      return this;
    }

    public Builder withAction(String label, String url, String screenReaderText) {
      actions.add(new SortableTableAction(label, url, screenReaderText));
      return this;
    }

    public SortableTableRow build() {
      return new SortableTableRow(List.copyOf(values), List.copyOf(actions));
    }
  }
}

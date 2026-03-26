package uk.co.nstauthority.licensingmanagementservice.mockups.contact;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SortableTableView(
    TableHeadingStyle tableHeadingStyle,
    Integer defaultSortIndex,
    Integer defaultSortDirection,
    String actionHeading,
    List<SortableTableRow> tableRows
) {
  private static final Logger LOGGER = LoggerFactory.getLogger(SortableTableView.class);

  public static SortableTableViewBuilder sortableTableBuilder() {
    return new SortableTableViewBuilder();
  }

  @Override
  public String toString() {
    try {
      return new ObjectMapper().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      LOGGER.error("Failed to serialize SortableTableView", e);
      return "";
    }
  }

  public static class SortableTableViewBuilder {
    private List<String> headings;
    private TableHeadingStyle headingStyle = TableHeadingStyle.COLUMN;
    private String actionHeading;
    private int defaultSortIndex = 0;
    private int defaultSortDirection = SortableTableSortDirection.ASCENDING.getFrontendSortValue();
    private final List<SortableTableRow> rows = new ArrayList<>();

    public SortableTableViewBuilder newWithHeadings(String... headings) {
      this.headings = Arrays.asList(headings);
      return this;
    }

    public SortableTableViewBuilder addRow(SortableTableRow row) {
      if (headings != null && row.rowValues().size() != headings.size()) {
        throw new IllegalArgumentException(
            "Row has %d values but table has %d headings".formatted(
                row.rowValues().size(), headings.size()));
      }
      rows.add(row);
      return this;
    }

    public SortableTableViewBuilder withHeadingStyle(TableHeadingStyle style) {
      this.headingStyle = style;
      return this;
    }

    public SortableTableViewBuilder withActionHeading(String actionHeading) {
      this.actionHeading = actionHeading;
      return this;
    }

    public SortableTableViewBuilder withDefaultSortIndex(int index) {
      this.defaultSortIndex = index;
      return this;
    }

    public SortableTableViewBuilder withDefaultSortDirection(SortableTableSortDirection direction) {
      this.defaultSortDirection = direction.getFrontendSortValue();
      return this;
    }

    public SortableTableView build() {
      var allRows = new ArrayList<SortableTableRow>();
      if (headings != null) {
        allRows.add(SortableTableRow.builder().withValues(headings.toArray(String[]::new)).build());
      }
      allRows.addAll(rows);
      return new SortableTableView(headingStyle, defaultSortIndex, defaultSortDirection, actionHeading, List.copyOf(allRows));
    }
  }
}

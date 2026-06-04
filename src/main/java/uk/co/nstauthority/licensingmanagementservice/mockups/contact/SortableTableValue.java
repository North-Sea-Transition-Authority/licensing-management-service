package uk.co.nstauthority.licensingmanagementservice.mockups.contact;

import java.util.List;

public record SortableTableValue(String value, Integer sortValue, String link, List<Tag> tags) {

  public SortableTableValue(String value) {
    this(value, null, null, List.of());
  }

  public SortableTableValue(String value, List<Tag> tags) {
    this(value, null, null, tags);
  }

  public SortableTableValue(Object value, String link, List<Tag> tags) {
    this(value == null ? "" : value.toString(), null, link, tags);
  }
}

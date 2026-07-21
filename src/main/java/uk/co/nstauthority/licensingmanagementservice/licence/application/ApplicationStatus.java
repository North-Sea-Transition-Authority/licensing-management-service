package uk.co.nstauthority.licensingmanagementservice.licence.application;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum ApplicationStatus implements Displayable {
  DRAFT("Draft", true),
  DELETED("Deleted", false),
  SUBMITTED("Submitted", true),
  ISSUE_DECISION("Issue decision", true),
  COMPLETE("Complete", true),
  WITHDRAWN("Withdrawn", true);

  private final String displayName;
  private final boolean searchable;

  ApplicationStatus(String displayName, boolean searchable) {
    this.displayName = displayName;
    this.searchable = searchable;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  public boolean isSearchable() {
    return searchable;
  }

  public static Set<ApplicationStatus> getSearchableStatuses() {
    return Arrays.stream(values())
        .filter(ApplicationStatus::isSearchable)
        .collect(Collectors.toUnmodifiableSet());
  }
}

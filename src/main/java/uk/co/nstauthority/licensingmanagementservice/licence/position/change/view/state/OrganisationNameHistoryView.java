package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The names an organisation was known by either side of the licence position date being viewed. The name it held on
 * the position date itself is deliberately excluded from both lists.
 */
public record OrganisationNameHistoryView(
    String organisationName,
    List<NameHistoryEntryView> previousNames,
    List<NameHistoryEntryView> laterNames
) implements Comparable<OrganisationNameHistoryView> {

  @Override
  public int compareTo(@NotNull OrganisationNameHistoryView other) {
    return String.CASE_INSENSITIVE_ORDER.compare(organisationName, other.organisationName);
  }

  public boolean hasNames() {
    return !previousNames.isEmpty() || !laterNames.isEmpty();
  }
}

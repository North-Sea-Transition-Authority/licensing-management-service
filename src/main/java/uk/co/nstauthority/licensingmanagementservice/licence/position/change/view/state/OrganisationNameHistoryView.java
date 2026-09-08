package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import java.util.List;

/**
 * The names an organisation was known by either side of the licence position date being viewed. The name it held on
 * the position date itself is deliberately excluded from both lists.
 */
public record OrganisationNameHistoryView(
    String organisationName,
    List<NameHistoryEntryView> previousNames,
    List<NameHistoryEntryView> laterNames
) {

  public boolean hasNames() {
    return !previousNames.isEmpty() || !laterNames.isEmpty();
  }
}

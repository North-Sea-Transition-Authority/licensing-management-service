package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

/**
 * A single historic name for an organisation, with the period boundary that name is anchored to already formatted for
 * display (for example {@code "to 3 March 1999"} or {@code "from 22 January 2022"}).
 */
public record NameHistoryEntryView(String name, String dateText) {
}

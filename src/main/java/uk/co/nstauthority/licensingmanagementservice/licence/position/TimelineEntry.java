package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.time.LocalDate;

/**
 * Internal timeline row used to sort licence position entries before mapping to the view.
 *
 * @param date the position/effective date, used for ordering
 * @param order the tie-break order within a date
 * @param view the fully-built timeline view for this row
 */
record TimelineEntry(LocalDate date, int order, LicencePositionTimelineView view) {
}
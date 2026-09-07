package uk.co.nstauthority.licensingmanagementservice.mockups.schedule;

/**
 * Stand-in for an extended {@code EventCommentView}, showing how a service generated comment would carry the licence it
 * refers to as separate reference and URL fields. Keeping the link out of the comment text means the text stays escaped
 * when rendered, and the URL is built from {@code ReverseRouter} at render time rather than being stored.
 *
 * @param linkedLicenceReference the referenced licence, or empty for a comment that does not reference one
 */
public record ScheduleCommentView(
    String comment,
    String author,
    String datetime,
    String removeCommentUrl,
    String linkedLicenceReference,
    String linkedLicenceUrl
) {
}

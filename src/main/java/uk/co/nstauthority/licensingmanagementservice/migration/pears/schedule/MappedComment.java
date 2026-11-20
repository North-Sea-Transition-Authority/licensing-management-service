package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

/**
 * A PEARS comment on a term, phase or rate, which becomes an LMS event comment.
 *
 * @param text  the comment, trimmed
 * @param isNew whether it is new or changed since the same event in the previous version. Only new comments are added,
 *              so the LMS comment history shows when each one first appeared without repeating it for every version
 */
public record MappedComment(String text, boolean isNew) {
}

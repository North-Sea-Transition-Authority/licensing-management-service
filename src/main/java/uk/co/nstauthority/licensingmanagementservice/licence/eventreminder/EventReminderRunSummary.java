package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

public record EventReminderRunSummary(
    int deadlinesDue,
    int deadlinesSuppressed,
    int batchesQueued,
    int batchesFailed
) {
}

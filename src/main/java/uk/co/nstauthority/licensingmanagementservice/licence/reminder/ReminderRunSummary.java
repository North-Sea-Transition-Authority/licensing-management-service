package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

public record ReminderRunSummary(
    int deadlinesDue,
    int deadlinesSuppressed,
    int batchesQueued,
    int batchesFailed
) {
}

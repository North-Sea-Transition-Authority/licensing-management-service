package uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments;

public record ScheduleCommentView(
    String comment,
    String author,
    String datetime,
    String removeCommentUrl
) {}

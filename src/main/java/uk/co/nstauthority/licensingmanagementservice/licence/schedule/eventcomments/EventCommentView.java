package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

public record EventCommentView(
    String comment,
    String author,
    String datetime
) {}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

public record TimelineWorkProgrammeActivityView(
    String category,
    String description,
    String dueDateString,
    String updateUrl,
    String deleteUrl
) implements ScheduleEvent {

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.WORK_PROGRAMME_ACTIVITY;
  }
}

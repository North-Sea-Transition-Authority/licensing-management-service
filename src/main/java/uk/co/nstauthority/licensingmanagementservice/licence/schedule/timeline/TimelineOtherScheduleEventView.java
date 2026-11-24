package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.RelinquishmentPercentageFormat;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public record TimelineOtherScheduleEventView(
    String category,
    OtherScheduleEventCategory categoryType,
    String description,
    String relinquishmentPercentage,
    LocalDate eventDate,
    String eventDateString,
    String updateUrl,
    String deleteUrl,
    String addCommentUrl,
    List<EventCommentView> comments,
    boolean showProgress
) implements ScheduleEvent {

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.OTHER;
  }

  @Override
  public LocalDate getSortingDate() {
    return eventDate;
  }

  @Override
  public Optional<ScheduleTimelineFilterOption> getFilterOption() {
    return Optional.of(categoryType == OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT
        ? ScheduleTimelineFilterOption.MANDATORY_RELINQUISHMENT
        : ScheduleTimelineFilterOption.OTHER);
  }

  public static ScheduleEvent getScheduleEventFrom(
      OtherScheduleEvent otherScheduleEvent,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, List<EventCommentView>> eventComments,
      LocalDate finalProgressDate
  ) {
    var eventDateString = otherScheduleEvent.getEventDate() != null
        ? DateFormatUtil.convertToDisplayText(otherScheduleEvent.getEventDate())
        : "";

    var editUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(OtherScheduleEventController.class)
        .renderUpdateEventForm(otherScheduleEvent.getId()))
        : "";

    var deleteUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
          .renderDeleteEventPage(otherScheduleEvent.getId()))
        : "";

    var addCommentUrl = allowedActions.contains(ScheduleEventAction.ADD_SCHEDULE_COMMENT)
        ? ReverseRouter.route(on(EventCommentController.class)
          .renderAddCommentForm(otherScheduleEvent.getId(), null))
        : "";

    var comments = eventComments.getOrDefault(otherScheduleEvent.getOriginalEventId(), List.of());

    var showProgress = otherScheduleEvent.getEventDate() != null
        && !otherScheduleEvent.getEventDate().isAfter(finalProgressDate);

    var relinquishmentPercentage = otherScheduleEvent.getRelinquishmentPercentage() != null
        ? "%s%%".formatted(RelinquishmentPercentageFormat.format(otherScheduleEvent.getRelinquishmentPercentage()))
        : null;

    return new TimelineOtherScheduleEventView(
        otherScheduleEvent.getCategoryString(),
        otherScheduleEvent.getCategory(),
        otherScheduleEvent.getDescription(),
        relinquishmentPercentage,
        otherScheduleEvent.getEventDate(),
        eventDateString,
        editUrl,
        deleteUrl,
        addCommentUrl,
        comments,
        showProgress
    );
  }

}

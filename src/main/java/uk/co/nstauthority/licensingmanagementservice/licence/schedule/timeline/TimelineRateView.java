package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.StartEndDates;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public record TimelineRateView(
    String title,
    LocalDate startDate,
    String startEndDateString,
    String rentalRateString,
    String updateUrl,
    String deleteUrl,
    String addCommentUrl,
    List<EventCommentView> comments,
    boolean showProgress
) implements ScheduleEvent {

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.RATE;
  }

  @Override
  public LocalDate getSortingDate() {
    return startDate;
  }

  public static ScheduleEvent getScheduleEventFrom(
      LicenceScheduleRate licenceScheduleRate,
      Map<UUID, StartEndDates> rateDatesMap,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, List<EventCommentView>> eventComments,
      LocalDate finalProgressDate
  ) {
    var rateDates = rateDatesMap.get(licenceScheduleRate.getId());
    var editUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(LicenceScheduleRateController.class)
          .renderUpdateLicenceScheduleRateForm(licenceScheduleRate.getId()))
        : "";

    var deleteUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(LicenceScheduleRateDeletionController.class)
          .renderDeleteRatePage(licenceScheduleRate.getId()))
        : "";

    var addCommentUrl = allowedActions.contains(ScheduleEventAction.ADD_SCHEDULE_COMMENT)
        ? ReverseRouter.route(on(EventCommentController.class)
          .renderAddCommentForm(licenceScheduleRate.getId(), null))
        : "";

    var comments = eventComments.getOrDefault(licenceScheduleRate.getOriginalEventId(), List.of());

    var showProgress = !rateDates.startDate().isAfter(finalProgressDate);

    return new TimelineRateView(
        generateTitle(licenceScheduleRate),
        rateDates.startDate(),
        generateStartEndDateString(rateDates),
        "£%s".formatted(licenceScheduleRate.getRentalRate().toString()),
        editUrl,
        deleteUrl,
        addCommentUrl,
        comments,
        showProgress
    );
  }

  public static String generateTitle(LicenceScheduleRate licenceScheduleRate) {
    if (licenceScheduleRate.getRateDefinitionOption().equals(RateDefinitionOption.TERM)) {
      var termType = licenceScheduleRate.getLicenceScheduleTerm().getTermType().getDisplayName();

      return "%s rate".formatted(termType);
    }

    if (licenceScheduleRate.getRateDefinitionOption().equals(RateDefinitionOption.PHASE)) {
      var phaseType = licenceScheduleRate.getLicenceSchedulePhase().getPhaseType().getDisplayName();

      return "%s rate".formatted(phaseType);
    }

    return "Rate";
  }

  static String generateStartEndDateString(StartEndDates dates) {
    return "%s to %s".formatted(
        DateFormatUtil.convertToDisplayText(dates.startDate()),
        DateFormatUtil.convertToDisplayText(dates.endDate())
    );
  }
}

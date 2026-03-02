package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public record TimelineRateView(
    String title,
    LocalDate startDate,
    String startDateString,
    String rentalRateString,
    String updateUrl,
    String deleteUrl
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
      List<ScheduleEventAction> allowedActions
  ) {
    var editUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(LicenceScheduleRateController.class)
          .renderUpdateLicenceScheduleRateForm(licenceScheduleRate.getId()))
        : "";

    var deleteUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(LicenceScheduleRateDeletionController.class)
          .renderDeleteRatePage(licenceScheduleRate.getId()))
        : "";

    return new TimelineRateView(
        generateTitle(licenceScheduleRate),
        licenceScheduleRate.getStartDate(),
        //TODO LMS1-195: change to duration once end date is calculated
        DateFormatUtil.convertToDisplayText(licenceScheduleRate.getStartDate()),
        "£%s".formatted(licenceScheduleRate.getRentalRate().toString()),
        editUrl,
        deleteUrl
    );
  }

  private static String generateTitle(LicenceScheduleRate licenceScheduleRate) {
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
}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeFeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Service
public class LicenceScheduleTimelineService {

  private final LicenceStartDateService licenceStartDateService;
  private final LicenceTypeFeatureService licenceTypeFeatureService;

  public LicenceScheduleTimelineService(LicenceStartDateService licenceStartDateService,
                                        LicenceTypeFeatureService licenceTypeFeatureService
  ) {
    this.licenceStartDateService = licenceStartDateService;
    this.licenceTypeFeatureService = licenceTypeFeatureService;
  }

  TimelineSummaryCardView getTimelineSummaryCardView(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail);

    return new TimelineSummaryCardView(DateFormatUtil.convertToDisplayText(licenceStartDate.getStartDate()));
  }

  List<TimelineActionView> getLicenceScheduleTimelineActions(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceType = licenceScheduleDetail.getLicenceSchedule().getLicence().getType();

    var actions = new LinkedList<TimelineActionView>();

    actions.add(
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_TERM,
            ReverseRouter.route(on(LicenceScheduleTermController.class).renderAddNewTermForm(licenceScheduleDetail.getId(), null))
        )
    );

    if (licenceTypeFeatureService.arePhasesCaptured(licenceType)) {
      actions.add(
          new TimelineActionView(
          LicenceScheduleTimelineAction.ADD_A_PHASE,
          ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderAddNewPhaseForm(licenceScheduleDetail.getId(), null))
          )
      );
    }

    return actions.stream()
        .sorted(Comparator.comparing(actionView -> actionView.action.getDisplayOrder()))
        .toList();
  }

  public record TimelineActionView(LicenceScheduleTimelineAction action, String url) {}
}

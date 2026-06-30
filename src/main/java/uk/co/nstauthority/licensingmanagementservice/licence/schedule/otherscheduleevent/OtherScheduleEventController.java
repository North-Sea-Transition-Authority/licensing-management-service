package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.licencescheduledetail.LicenceScheduleDetailHasStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.LicenceScheduleRelativeOptionsService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/licence/schedule")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.SCHEDULE_ADMINISTRATOR}, teamType = TeamType.LICENCE_MANAGEMENT)
})
@LicenceScheduleDetailHasStatus(value = LicenceScheduleDetailStatus.DRAFT)
public class OtherScheduleEventController {

  private final OtherScheduleEventService otherScheduleEventService;
  private final OtherScheduleEventFormService otherScheduleEventFormService;
  private final OtherScheduleEventFormValidator otherScheduleEventFormValidator;
  private final LicenceService licenceService;
  private final LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;

  public OtherScheduleEventController(
      OtherScheduleEventService otherScheduleEventService,
      OtherScheduleEventFormService otherScheduleEventFormService,
      OtherScheduleEventFormValidator otherScheduleEventFormValidator,
      LicenceService licenceService,
      LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService
  ) {
    this.otherScheduleEventService = otherScheduleEventService;
    this.otherScheduleEventFormService = otherScheduleEventFormService;
    this.otherScheduleEventFormValidator = otherScheduleEventFormValidator;
    this.licenceService = licenceService;
    this.licenceScheduleRelativeOptionsService = licenceScheduleRelativeOptionsService;
  }

  @GetMapping("/{licenceScheduleDetailId}/other-schedule-event/create")
  public ModelAndView renderAddNewEventForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return getActivityModelAndView(new OtherScheduleEventForm(), licenceScheduleDetail);
  }

  @PostMapping("/{licenceScheduleDetailId}/other-schedule-event/create")
  ModelAndView submitAddNewEventForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("form") OtherScheduleEventForm form,
      BindingResult bindingResult,
      ServiceUserDetail serviceUserDetail
  ) {
    if (!otherScheduleEventFormValidator.isValid(form, bindingResult)) {
      return getActivityModelAndView(form, licenceScheduleDetail);
    }

    otherScheduleEventFormService.saveEventFromForm(
        form,
        licenceScheduleDetail,
        new OtherScheduleEvent(),
        serviceUserDetail
    );

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

  @GetMapping("other-schedule-event/{otherScheduleEventId}/update")
  public ModelAndView renderUpdateEventForm(
      @PathVariable UUID otherScheduleEventId
  ) {
    var event = otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEventId);

    return getActivityModelAndView(
        otherScheduleEventFormService.getEventForm(event),
        event.getLicenceScheduleDetail()
    );
  }

  @PostMapping("other-schedule-event/{otherScheduleEventId}/update")
  ModelAndView submitUpdateEventForm(
      @PathVariable UUID otherScheduleEventId,
      @ModelAttribute("form") OtherScheduleEventForm form,
      BindingResult bindingResult,
      ServiceUserDetail serviceUserDetail
  ) {
    var event = otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEventId);
    var licenceScheduleDetail = event.getLicenceScheduleDetail();

    if (!otherScheduleEventFormValidator.isValid(form, bindingResult)) {
      return getActivityModelAndView(form, licenceScheduleDetail);
    }

    otherScheduleEventFormService.saveEventFromForm(
        form,
        licenceScheduleDetail,
        event,
        serviceUserDetail
    );

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

  private ModelAndView getActivityModelAndView(OtherScheduleEventForm form, LicenceScheduleDetail licenceScheduleDetail) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();

    return new ModelAndView("lms/licence/schedule/createOtherScheduleEvent")
        .addObject("form", form)
        .addObject("categoryRadioOptions", OtherScheduleEventCategory.getCategoriesForLicenceType(licence.getType()))
        .addObject("eventDateRadioOptions", otherScheduleEventFormService.getDateOptions(licenceScheduleDetail))
        .addObject("termOptions", licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail))
        .addObject("phaseOptions", licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail))
        .addObject("relativeOptions", licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail))
        .addObject("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl())
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence));
  }

}

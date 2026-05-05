package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.licencescheduledetail;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;

@Component
@Order(9)
public class LicenceScheduleDetailHasStatusInterceptorRule implements AccessInterceptorRule {

  static final String LICENCE_SCHEDULE_DETAIL_ID = "licenceScheduleDetailId";
  static final String LICENCE_SCHEDULE_TERM_ID = "licenceScheduleTermId";
  static final String LICENCE_SCHEDULE_PHASE_ID = "licenceSchedulePhaseId";
  static final String LICENCE_SCHEDULE_RATE_ID = "licenceScheduleRateId";
  static final String WORK_PROGRAMME_ACTIVITY_ID = "workProgrammeActivityId";
  static final String OTHER_SCHEDULE_EVENT_ID = "otherScheduleEventId";

  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceScheduleRateService licenceScheduleRateService;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final OtherScheduleEventService otherScheduleEventService;

  @Autowired
  public LicenceScheduleDetailHasStatusInterceptorRule(
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceScheduleRateService licenceScheduleRateService,
      WorkProgrammeActivityService workProgrammeActivityService,
      OtherScheduleEventService otherScheduleEventService
  ) {
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.otherScheduleEventService = otherScheduleEventService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return LicenceScheduleDetailHasStatus.class;
  }

  @Override
  public SecurityRuleResult check(
      Object annotation,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    var hasStatus = (LicenceScheduleDetailHasStatus) annotation;

    if (hasStatus.value().length == 0) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "No statuses provided to security annotation"
      );
    }

    var licenceScheduleDetail = getDetailFromRequest(request);

    for (LicenceScheduleDetailStatus status : hasStatus.value()) {
      if (status.equals(licenceScheduleDetail.getStatus())) {
        return SecurityRuleResult.continueAsNormal();
      }
    }

    return SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.FORBIDDEN,
        "LicenceScheduleDetail with id %s is not in an expected status".formatted(licenceScheduleDetail.getId())
    );
  }

  private LicenceScheduleDetail getDetailFromRequest(HttpServletRequest request) {
    Map<String, String> pathVariables =
        (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

    if (pathVariables != null) {
      if (pathVariables.containsKey(LICENCE_SCHEDULE_DETAIL_ID)) {
        var detailId = UUID.fromString(pathVariables.get(LICENCE_SCHEDULE_DETAIL_ID));
        return licenceScheduleDetailService.getByIdOrThrow(detailId);
      }

      if (pathVariables.containsKey(LICENCE_SCHEDULE_TERM_ID)) {
        var termId = UUID.fromString(pathVariables.get(LICENCE_SCHEDULE_TERM_ID));
        var detailId = licenceScheduleTermService.getTermByIdOrThrow(termId).getLicenceScheduleDetail().getId();
        return licenceScheduleDetailService.getByIdOrThrow(detailId);
      }

      if (pathVariables.containsKey(LICENCE_SCHEDULE_PHASE_ID)) {
        var phaseId = UUID.fromString(pathVariables.get(LICENCE_SCHEDULE_PHASE_ID));
        var detailId = licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId).getLicenceScheduleDetail().getId();
        return licenceScheduleDetailService.getByIdOrThrow(detailId);
      }

      if (pathVariables.containsKey(LICENCE_SCHEDULE_RATE_ID)) {
        var rateId = UUID.fromString(pathVariables.get(LICENCE_SCHEDULE_RATE_ID));
        var detailId = licenceScheduleRateService.getRateByIdOrThrow(rateId).getLicenceScheduleDetail().getId();
        return licenceScheduleDetailService.getByIdOrThrow(detailId);
      }

      if (pathVariables.containsKey(WORK_PROGRAMME_ACTIVITY_ID)) {
        var activityId = UUID.fromString(pathVariables.get(WORK_PROGRAMME_ACTIVITY_ID));
        var detailId = workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(activityId)
            .getLicenceScheduleDetail()
            .getId();
        return licenceScheduleDetailService.getByIdOrThrow(detailId);
      }

      if (pathVariables.containsKey(OTHER_SCHEDULE_EVENT_ID)) {
        var eventId = UUID.fromString(pathVariables.get(OTHER_SCHEDULE_EVENT_ID));
        var detailId = otherScheduleEventService.getOtherScheduleEventByIdOrThrow(eventId).getLicenceScheduleDetail().getId();
        return licenceScheduleDetailService.getByIdOrThrow(detailId);
      }
    }

    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No licenceScheduleDetail or event id found in path variables");
  }
}
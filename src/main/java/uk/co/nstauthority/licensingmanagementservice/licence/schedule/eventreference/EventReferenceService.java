package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference;

import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineRateView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;

@Service
public class EventReferenceService {

  private final EventReferenceRepository eventReferenceRepository;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceScheduleRateService licenceScheduleRateService;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final OtherScheduleEventService otherScheduleEventService;

  public EventReferenceService(
      EventReferenceRepository eventReferenceRepository,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceScheduleRateService licenceScheduleRateService,
      WorkProgrammeActivityService workProgrammeActivityService,
      OtherScheduleEventService otherScheduleEventService
  ) {
    this.eventReferenceRepository = eventReferenceRepository;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.otherScheduleEventService = otherScheduleEventService;
  }

  public EventReference getEventReferenceByIdOrThrow(UUID id) {
    return eventReferenceRepository.findById(id)
        .orElseThrow(() -> new LmsEntityNotFoundException("EventReference", id));
  }

  @Transactional
  public EventReference createEventReference(LicenceSchedule licenceSchedule) {
    var eventReference = new EventReference();
    eventReference.setLicenceSchedule(licenceSchedule);
    return eventReferenceRepository.save(eventReference);
  }

  public String getEventReferenceEventCaption(
      EventReference eventReference,
      ScheduleEventType eventType
  ) {
    var scheduleDetail = licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
        eventReference.getLicenceSchedule().getLicence(),
        LicenceScheduleDetailStatus.ACTIVE
    );

    return switch (eventType) {
      case TERM -> getEventCaptionForTerm(scheduleDetail, eventReference);
      case PHASE -> getEventCaptionForPhase(scheduleDetail, eventReference);
      case RATE -> getEventCaptionForRate(scheduleDetail, eventReference);
      case WORK_PROGRAMME_ACTIVITY -> getEventCaptionForActivity(scheduleDetail, eventReference);
      case OTHER -> getEventCaptionForEvent(scheduleDetail, eventReference);
    };
  }

  private String getEventCaptionForTerm(
      LicenceScheduleDetail scheduleDetail,
      EventReference eventReference
  ) {
    var term = licenceScheduleTermService.getTermByScheduleDetailAndEventReferenceOrThrow(scheduleDetail, eventReference);

    return term.getTermType().getDisplayName();
  }

  private String getEventCaptionForPhase(
      LicenceScheduleDetail scheduleDetail,
      EventReference eventReference
  ) {
    var phase = licenceSchedulePhaseService.getPhaseByScheduleDetailAndEventReferenceOrThrow(scheduleDetail, eventReference);

    return phase.getPhaseType().getDisplayName();
  }

  private String getEventCaptionForRate(
      LicenceScheduleDetail scheduleDetail,
      EventReference eventReference
  ) {
    var rate = licenceScheduleRateService.getRateByScheduleDetailAndEventReferenceOrThrow(scheduleDetail, eventReference);

    return TimelineRateView.generateTitle(rate);
  }

  private String getEventCaptionForActivity(
      LicenceScheduleDetail scheduleDetail,
      EventReference eventReference
  ) {
    var activity = workProgrammeActivityService.getWorkProgrammeActivityByScheduleDetailAndEventReferenceOrThrow(
        scheduleDetail,
        eventReference
    );

    return activity.getCategoryString();
  }

  private String getEventCaptionForEvent(
      LicenceScheduleDetail scheduleDetail,
      EventReference eventReference
  ) {
    var event = otherScheduleEventService.getOtherScheduleEventByScheduleDetailAndEventReferenceOrThrow(
        scheduleDetail,
        eventReference
    );

    return event.getCategoryString();
  }
}

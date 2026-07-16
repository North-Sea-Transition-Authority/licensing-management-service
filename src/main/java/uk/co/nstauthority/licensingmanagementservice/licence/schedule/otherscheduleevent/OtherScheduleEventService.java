package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import static uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption.WITHIN_A_PHASE;
import static uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption.WITHIN_A_TERM;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.ScheduleState;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Service
public class OtherScheduleEventService {

  private final OtherScheduleEventRepository otherScheduleEventRepository;
  private final EventCommentService eventCommentService;

  public OtherScheduleEventService(
      OtherScheduleEventRepository otherScheduleEventRepository,
      EventCommentService eventCommentService
  ) {
    this.otherScheduleEventRepository = otherScheduleEventRepository;
    this.eventCommentService = eventCommentService;
  }

  public OtherScheduleEvent getOtherScheduleEventByIdOrThrow(UUID id) {
    return otherScheduleEventRepository.findById(id)
        .orElseThrow(() -> new LmsEntityNotFoundException("OtherScheduleEvent", id));
  }

  public List<OtherScheduleEvent> getOtherScheduleEvents(LicenceScheduleDetail licenceScheduleDetail) {
    return otherScheduleEventRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail);
  }

  @Transactional
  public void saveScheduleEvents(List<OtherScheduleEvent> otherScheduleEvents) {
    otherScheduleEventRepository.saveAll(otherScheduleEvents);
  }

  public List<OtherScheduleEvent> getScheduleEventsByTermAndDateOption(
      LicenceScheduleTerm licenceScheduleTerm,
      OtherScheduleEventDateOption dateOption
  ) {
    return otherScheduleEventRepository.findAllByLicenceScheduleTermAndDateOption(licenceScheduleTerm, dateOption);
  }

  public List<OtherScheduleEvent> getScheduleEventsByPhaseAndDateOption(
      LicenceSchedulePhase licenceSchedulePhase,
      OtherScheduleEventDateOption dateOption
  ) {
    return otherScheduleEventRepository.findAllByLicenceSchedulePhaseAndDateOption(licenceSchedulePhase, dateOption);
  }

  public List<OtherScheduleEvent> getScheduleEventsByDateRangeFor(LicenceScheduleTerm licenceScheduleTerm) {
    return getScheduleEventsByDateRange(
        licenceScheduleTerm.getLicenceScheduleDetail(),
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate()
    );
  }

  public List<OtherScheduleEvent> getScheduleEventsByDateRangeFor(LicenceSchedulePhase licenceSchedulePhase) {
    return getScheduleEventsByDateRange(
        licenceSchedulePhase.getLicenceScheduleDetail(),
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate()
    );
  }

  private List<OtherScheduleEvent> getScheduleEventsByDateRange(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate from,
      LocalDate to
  ) {
    return otherScheduleEventRepository.findAllByLicenceScheduleDetailAndEventDateBetween(licenceScheduleDetail, from, to);
  }

  public List<OtherScheduleEvent> getEventsAfterDate(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate date
  ) {
    return otherScheduleEventRepository.findAllByLicenceScheduleDetailAndEventDateAfter(licenceScheduleDetail, date);
  }

  public List<OtherScheduleEvent> getAllEventsLinkedTo(LicenceScheduleTerm licenceScheduleTerm) {
    return otherScheduleEventRepository.findAllByLicenceScheduleTerm(licenceScheduleTerm);
  }

  public List<OtherScheduleEvent> getAllEventsLinkedTo(LicenceSchedulePhase licenceSchedulePhase) {
    return otherScheduleEventRepository.findAllByLicenceSchedulePhase(licenceSchedulePhase);
  }

  @Transactional
  public void deleteOtherScheduleEvent(OtherScheduleEvent otherScheduleEvent) {
    eventCommentService.deletePendingCommentForScheduleEvent(otherScheduleEvent);
    otherScheduleEventRepository.delete(otherScheduleEvent);
  }

  public boolean hasEventWithinScheduleWindow(
      LicenceScheduleDetail licenceScheduleDetail,
      OtherScheduleEventCategory category,
      ScheduleState scheduleState
  ) {
    var hasWithinTermEvent = Stream.of(scheduleState.currentTerm(), scheduleState.nextTerm())
        .flatMap(term -> otherScheduleEventRepository
            .findAllByLicenceScheduleTermAndDateOption(term, WITHIN_A_TERM).stream())
        .anyMatch(event -> event.getCategory() == category);

    if (hasWithinTermEvent) {
      return true;
    }

    var hasWithinPhaseEvent = Stream.of(scheduleState.currentPhase(), scheduleState.nextPhase())
        .flatMap(phase -> otherScheduleEventRepository
            .findAllByLicenceSchedulePhaseAndDateOption(phase, WITHIN_A_PHASE).stream())
        .anyMatch(event -> event.getCategory() == category);

    if (hasWithinPhaseEvent) {
      return true;
    }

    return otherScheduleEventRepository
        .findAllByLicenceScheduleDetailAndEventDateBetween(
            licenceScheduleDetail,
            getScheduleWindowStart(scheduleState),
            getScheduleWindowEnd(scheduleState)
        )
        .stream()
        .anyMatch(event -> event.getCategory() == category);
  }

  private LocalDate getScheduleWindowStart(ScheduleState scheduleState) {
    return scheduleState.currentTerm().getStartDate();
  }

  private LocalDate getScheduleWindowEnd(ScheduleState scheduleState) {
    if (scheduleState.nextTerm() != null) {
      return scheduleState.nextTerm().getEndDate();
    }
    return scheduleState.nextPhase() != null ? scheduleState.nextPhase().getEndDate() : null;
  }
}

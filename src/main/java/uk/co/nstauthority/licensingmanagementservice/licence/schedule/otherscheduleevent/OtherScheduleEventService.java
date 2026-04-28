package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Service
public class OtherScheduleEventService {
  
  private final OtherScheduleEventRepository otherScheduleEventRepository;

  public OtherScheduleEventService(OtherScheduleEventRepository otherScheduleEventRepository) {
    this.otherScheduleEventRepository = otherScheduleEventRepository;
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

  public List<OtherScheduleEvent> getActiveScheduleEventsByTermAndDateOption(
      LicenceScheduleTerm licenceScheduleTerm,
      OtherScheduleEventDateOption dateOption
  ) {
    return otherScheduleEventRepository.findAllByLicenceScheduleTermAndDateOptionAndStatus(
        licenceScheduleTerm,
        dateOption,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  public List<OtherScheduleEvent> getActiveScheduleEventsByPhaseAndDateOption(
      LicenceSchedulePhase licenceSchedulePhase,
      OtherScheduleEventDateOption dateOption
  ) {
    return otherScheduleEventRepository.findAllByLicenceSchedulePhaseAndDateOptionAndStatus(
        licenceSchedulePhase,
        dateOption,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  public List<OtherScheduleEvent> getActiveScheduleEventsByDateRangeFor(LicenceScheduleTerm licenceScheduleTerm) {
    return getActiveScheduleEventsByDateRange(
        licenceScheduleTerm.getLicenceScheduleDetail(),
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate()
    );
  }

  public List<OtherScheduleEvent> getActiveScheduleEventsByDateRangeFor(LicenceSchedulePhase licenceSchedulePhase) {
    return getActiveScheduleEventsByDateRange(
        licenceSchedulePhase.getLicenceScheduleDetail(),
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate()
    );
  }

  private List<OtherScheduleEvent> getActiveScheduleEventsByDateRange(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate from,
      LocalDate to
  ) {
    return otherScheduleEventRepository.findAllByLicenceScheduleDetailAndEventDateBetweenAndStatus(
        licenceScheduleDetail,
        from,
        to,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  public List<OtherScheduleEvent> getActiveEventsAfterDate(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate date
  ) {
    return otherScheduleEventRepository.findAllByLicenceScheduleDetailAndEventDateAfterAndStatus(
        licenceScheduleDetail,
        date,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  @Transactional
  public void deleteOtherScheduleEvent(OtherScheduleEvent otherScheduleEvent) {
    otherScheduleEvent.setStatus(LicenceScheduleEventStatus.DELETED);
    otherScheduleEventRepository.save(otherScheduleEvent);
  }
}

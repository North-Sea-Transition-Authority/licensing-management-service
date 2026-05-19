package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;

@Service
public class EventReferenceService {

  private final EventReferenceRepository eventReferenceRepository;

  public EventReferenceService(EventReferenceRepository eventReferenceRepository) {
    this.eventReferenceRepository = eventReferenceRepository;
  }

  @Transactional
  public EventReference createEventReference(LicenceSchedule licenceSchedule) {
    var eventReference = new EventReference();
    eventReference.setLicenceSchedule(licenceSchedule);
    return eventReferenceRepository.save(eventReference);
  }

}

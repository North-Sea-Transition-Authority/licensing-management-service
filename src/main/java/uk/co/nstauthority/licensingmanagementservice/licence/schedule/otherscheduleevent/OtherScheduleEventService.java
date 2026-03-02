package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;

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
}

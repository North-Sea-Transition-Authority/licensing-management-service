package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;

@Repository
public interface EventCommentRepository extends JpaRepository<EventComment, UUID>, NotDuplicationSource {

  List<EventComment> getAllByEventReference_LicenceScheduleAndStatus(
      LicenceSchedule licenceSchedule,
      EventCommentStatus status
  );

  void deleteAllByEventReference_LicenceScheduleAndStatus(LicenceSchedule licenceSchedule, EventCommentStatus status);

  Optional<EventComment> findByEventReferenceAndStatus(EventReference eventReference, EventCommentStatus status);

}

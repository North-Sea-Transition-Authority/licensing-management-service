package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;

@Repository
public interface EventCommentRepository extends JpaRepository<EventComment, UUID>, NotDuplicationSource {

  List<EventComment> getAllByEventReference_LicenceSchedule(LicenceSchedule licenceSchedule);

}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;

@Repository
public interface ScheduleCommentRepository extends JpaRepository<ScheduleComment, UUID>, NotDuplicationSource {

  List<ScheduleComment> findAllByLicenceScheduleOrderByTimestamp(LicenceSchedule licenceSchedule);

}

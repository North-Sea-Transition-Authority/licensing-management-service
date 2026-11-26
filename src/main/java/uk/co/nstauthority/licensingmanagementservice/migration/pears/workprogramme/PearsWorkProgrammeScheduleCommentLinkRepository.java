package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface PearsWorkProgrammeScheduleCommentLinkRepository
    extends JpaRepository<PearsWorkProgrammeScheduleCommentLink, UUID>, NotDuplicationSource {
}

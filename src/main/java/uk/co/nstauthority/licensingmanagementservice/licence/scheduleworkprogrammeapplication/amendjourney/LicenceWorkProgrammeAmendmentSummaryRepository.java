package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Repository
public interface LicenceWorkProgrammeAmendmentSummaryRepository
    extends JpaRepository<LicenceWorkProgrammeAmendmentSummary, UUID>, DuplicationSource<ScheduleWorkProgrammeApplicationDetail> {

  @DuplicateThisOnUpdate
  Optional<LicenceWorkProgrammeAmendmentSummary>
      findLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail);
}
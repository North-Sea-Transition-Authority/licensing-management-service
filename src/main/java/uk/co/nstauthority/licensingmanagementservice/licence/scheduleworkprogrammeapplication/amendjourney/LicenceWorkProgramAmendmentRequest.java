package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Audited
@Entity(name = "licence_work_programme_amendment_request")
public class LicenceWorkProgramAmendmentRequest {

  @Id
  @UuidGenerator
  private UUID id;

  private UUID workProgrammeActivityId;

  @ManyToOne
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ScheduleWorkProgrammeApplicationDetail getScheduleWorkProgrammeApplicationDetails() {
    return scheduleWorkProgrammeApplicationDetails;
  }

  public void setScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails) {
    this.scheduleWorkProgrammeApplicationDetails = scheduleWorkProgrammeApplicationDetails;
  }

  public UUID getWorkProgrammeActivityId() {
    return workProgrammeActivityId;
  }

  public void setWorkProgrammeActivityId(UUID workProgrammeId) {
    this.workProgrammeActivityId = workProgrammeId;
  }
}
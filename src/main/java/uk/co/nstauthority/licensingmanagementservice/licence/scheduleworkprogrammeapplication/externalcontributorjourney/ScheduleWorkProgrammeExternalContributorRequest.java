package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.externalcontributorjourney;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;

@Audited
@Entity(name = "swp_external_contributor_request")
public class ScheduleWorkProgrammeExternalContributorRequest {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "schedule_work_programme_application_id")
  private ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication;

  private Boolean addExternalContributors;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ScheduleWorkProgrammeApplication getScheduleWorkProgrammeApplication() {
    return scheduleWorkProgrammeApplication;
  }

  public void setScheduleWorkProgrammeApplication(ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication) {
    this.scheduleWorkProgrammeApplication = scheduleWorkProgrammeApplication;
  }

  public Boolean getAddExternalContributors() {
    return addExternalContributors;
  }

  public void setAddExternalContributors(Boolean addExternalContributors) {
    this.addExternalContributors = addExternalContributors;
  }
}

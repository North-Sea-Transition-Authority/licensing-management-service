package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ScheduleApplication;

@Audited
@Entity(name = "schedule_work_programme_applications")
public class ScheduleWorkProgrammeApplication extends ScheduleApplication {

  @Column
  private Long stewardWuaId;

  public Long getStewardWuaId() {
    return stewardWuaId;
  }

  public void setStewardWuaId(Long stewardWuaId) {
    this.stewardWuaId = stewardWuaId;
  }

  @Override
  public ApplicationType getApplicationType() {
    return ApplicationType.SCHEDULE_AMENDMENT_APPLICATION;
  }
}

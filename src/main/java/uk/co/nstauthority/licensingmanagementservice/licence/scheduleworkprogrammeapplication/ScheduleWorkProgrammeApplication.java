package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ScheduleApplication;

@Audited
@Entity
@DiscriminatorValue("SCHEDULE_AMENDMENT_APPLICATION")
public class ScheduleWorkProgrammeApplication extends ScheduleApplication {

  @OneToOne(mappedBy = "scheduleWorkProgrammeApplication", cascade = CascadeType.ALL)
  private ScheduleWorkProgrammeApplicationData applicationData;

  public Long getStewardWuaId() {
    return applicationData == null ? null : applicationData.getStewardWuaId();
  }

  public void setStewardWuaId(Long stewardWuaId) {
    if (applicationData == null) {
      applicationData = new ScheduleWorkProgrammeApplicationData(this);
    }
    applicationData.setStewardWuaId(stewardWuaId);
  }

  @Override
  public ApplicationType getApplicationType() {
    return ApplicationType.SCHEDULE_AMENDMENT_APPLICATION;
  }
}

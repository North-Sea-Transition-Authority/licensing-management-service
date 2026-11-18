package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ScheduleApplication;

@Audited
@Entity
@DiscriminatorValue("CONTINUATION_APPLICATION")
public class LicenceContinuationApplication extends ScheduleApplication {

  @Override
  public ApplicationType getApplicationType() {
    return ApplicationType.CONTINUATION_APPLICATION;
  }
}

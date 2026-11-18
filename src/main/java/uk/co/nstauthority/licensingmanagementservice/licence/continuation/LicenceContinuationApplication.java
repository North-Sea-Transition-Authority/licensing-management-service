package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ScheduleApplication;

@Audited
@Entity(name = "licence_continuation_applications")
public class LicenceContinuationApplication extends ScheduleApplication {

  @Column
  private String withdrawalReason;

  @Column
  private Instant withdrawnDatetime;

  public String getWithdrawalReason() {
    return withdrawalReason;
  }

  public void setWithdrawalReason(String withdrawalReason) {
    this.withdrawalReason = withdrawalReason;
  }

  public Instant getWithdrawnDatetime() {
    return withdrawnDatetime;
  }

  public void setWithdrawnDatetime(Instant withdrawnDatetime) {
    this.withdrawnDatetime = withdrawnDatetime;
  }

  @Override
  public ApplicationType getApplicationType() {
    return ApplicationType.CONTINUATION_APPLICATION;
  }
}

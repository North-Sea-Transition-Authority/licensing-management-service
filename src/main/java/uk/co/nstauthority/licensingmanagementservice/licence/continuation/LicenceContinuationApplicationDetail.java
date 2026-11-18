package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableEntity;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Audited
@Entity(name = "licence_continuation_application_details")
@DiscriminatorValue("CONTINUATION_APPLICATION")
@PathVariableEntity(pathVariableName = LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID)
public class LicenceContinuationApplicationDetail extends ApplicationDetail {

  public static final String LICENCE_CONTINUATION_APPLICATION_DETAIL_ID = "licenceContinuationApplicationDetailId";

  @ManyToOne
  @JoinColumn(name = "current_term_id")
  private LicenceScheduleTerm currentTerm;

  @ManyToOne
  @JoinColumn(name = "current_phase_id")
  private LicenceSchedulePhase currentPhase;

  @ManyToOne
  @JoinColumn(name = "next_term_id")
  private LicenceScheduleTerm nextTerm;

  @ManyToOne
  @JoinColumn(name = "next_phase_id")
  private LicenceSchedulePhase nextPhase;

  public LicenceContinuationApplicationDetail() {
  }

  @VisibleForTesting
  public LicenceContinuationApplicationDetail(UUID id) {
    super(id);
  }

  public LicenceContinuationApplication getLicenceContinuationApplication() {
    return (LicenceContinuationApplication) Hibernate.unproxy(getApplication());
  }

  public void setLicenceContinuationApplication(
      LicenceContinuationApplication licenceContinuationApplication) {
    setApplication(licenceContinuationApplication);
  }

  public LicenceScheduleTerm getCurrentTerm() {
    return currentTerm;
  }

  public void setCurrentTerm(LicenceScheduleTerm currentTerm) {
    this.currentTerm = currentTerm;
  }

  public LicenceSchedulePhase getCurrentPhase() {
    return currentPhase;
  }

  public void setCurrentPhase(LicenceSchedulePhase currentPhase) {
    this.currentPhase = currentPhase;
  }

  public LicenceScheduleTerm getNextTerm() {
    return nextTerm;
  }

  public void setNextTerm(LicenceScheduleTerm nextTerm) {
    this.nextTerm = nextTerm;
  }

  public LicenceSchedulePhase getNextPhase() {
    return nextPhase;
  }

  public void setNextPhase(LicenceSchedulePhase nextPhase) {
    this.nextPhase = nextPhase;
  }
}

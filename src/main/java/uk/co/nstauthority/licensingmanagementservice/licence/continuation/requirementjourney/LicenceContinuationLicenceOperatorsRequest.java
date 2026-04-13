package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Audited
@Entity
@Table(name = "licence_continuation_licence_operators_request")
public class LicenceContinuationLicenceOperatorsRequest {

  @Id
  @UuidGenerator
  private UUID id;

  @OneToOne
  @JoinColumn(name = "licence_continuation_application_detail_id")
  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;

  private String pendingActionsExplanation;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LicenceContinuationApplicationDetail getLicenceContinuationApplicationDetail() {
    return licenceContinuationApplicationDetail;
  }

  public void setLicenceContinuationApplicationDetail(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    this.licenceContinuationApplicationDetail = licenceContinuationApplicationDetail;
  }

  public String getPendingActionsExplanation() {
    return pendingActionsExplanation;
  }

  public void setPendingActionsExplanation(String pendingActionsExplanation) {
    this.pendingActionsExplanation = pendingActionsExplanation;
  }
}
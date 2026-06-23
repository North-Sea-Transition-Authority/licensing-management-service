package uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Audited
@Entity(name = "licence_continuation_external_contributor_request")
public class LicenceContinuationExternalContributorRequest {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_continuation_application_detail_id")
  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;

  private Boolean addExternalContributors;

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

  public Boolean getAddExternalContributors() {
    return addExternalContributors;
  }

  public void setAddExternalContributors(Boolean addExternalContributors) {
    this.addExternalContributors = addExternalContributors;
  }
}

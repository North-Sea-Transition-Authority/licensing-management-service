package uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;

@Audited
@Entity(name = "licence_continuation_external_contributor_request")
public class LicenceContinuationExternalContributorRequest {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_continuation_application_id")
  private LicenceContinuationApplication licenceContinuationApplication;

  private Boolean addExternalContributors;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LicenceContinuationApplication getLicenceContinuationApplication() {
    return licenceContinuationApplication;
  }

  public void setLicenceContinuationApplication(
      LicenceContinuationApplication licenceContinuationApplication) {
    this.licenceContinuationApplication = licenceContinuationApplication;
  }

  public Boolean getAddExternalContributors() {
    return addExternalContributors;
  }

  public void setAddExternalContributors(Boolean addExternalContributors) {
    this.addExternalContributors = addExternalContributors;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;

@Audited
@Entity(name = "licence_contact")
public class LicenceContact {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "licence_id"),
      @JoinColumn(name = "responsible_organisation_id")
  })
  private LicenceResponsibleOrganisation licensee;

  @Column(name = "contact_email")
  private String contactEmail;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LicenceResponsibleOrganisation getLicensee() {
    return licensee;
  }

  public void setLicensee(LicenceResponsibleOrganisation licensee) {
    this.licensee = licensee;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.envers.Audited;

@Audited
@Entity(name = "licence_responsible_organisations")
@IdClass(LicenceOrganisationId.class)
public class ResponsibleOrganisation {

  @Id
  @JoinColumn(name = "licence_id")
  @ManyToOne
  private Licence licence;

  @Id
  private Integer responsibleOrganisationId;

  private Boolean managedByLms;

  public ResponsibleOrganisation() {
  }

  public Licence getLicence() {
    return licence;
  }

  public void setLicence(Licence licence) {
    this.licence = licence;
  }

  public Integer getResponsibleOrganisationId() {
    return responsibleOrganisationId;
  }

  public void setResponsibleOrganisationId(Integer responsibleOrganisationId) {
    this.responsibleOrganisationId = responsibleOrganisationId;
  }

  public Boolean getManagedByLms() {
    return managedByLms;
  }

  public void setManagedByLms(Boolean managedByLms) {
    this.managedByLms = managedByLms;
  }
}

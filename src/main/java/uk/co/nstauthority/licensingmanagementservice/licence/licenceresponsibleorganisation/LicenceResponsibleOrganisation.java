package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Audited
@Entity(name = "licence_responsible_organisations")
@IdClass(LicenceOrganisationId.class)
public class LicenceResponsibleOrganisation {

  @Id
  @ManyToOne
  @JoinColumn(name = "licence_id")
  @Cascade(CascadeType.MERGE)
  private Licence licence;

  @Id
  private Integer responsibleOrganisationId;

  private Boolean managedByLms;

  public LicenceResponsibleOrganisation() {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LicenceResponsibleOrganisation that = (LicenceResponsibleOrganisation) o;
    return Objects.equals(licence, that.licence)
        && Objects.equals(responsibleOrganisationId, that.responsibleOrganisationId)
        && Objects.equals(managedByLms, that.managedByLms);
  }

  @Override
  public int hashCode() {
    return Objects.hash(licence, responsibleOrganisationId, managedByLms);
  }
}

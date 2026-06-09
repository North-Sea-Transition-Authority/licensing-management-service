package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.util.Objects;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableEntity;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.responsibleteam.LicenceTeam;

@Audited
@Entity(name = "licences")
@PathVariableEntity(pathVariableName = LicenceArgumentResolver.LICENCE_ID)
public class Licence {

  @Id
  private Integer id;

  @Enumerated(EnumType.STRING)
  private LicenceType type;

  @Enumerated(EnumType.STRING)
  private LicenceSubtype subtype;

  private String prefix;

  private String licenceNumber;

  private String licenceReference;

  private String roundIssuedOn;

  @Enumerated(EnumType.STRING)
  private LicenceStatus status;

  @Enumerated(EnumType.STRING)
  private LicenceTeam responsibleTeam;

  public Licence() {
  }

  public Licence(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public LicenceType getType() {
    return type;
  }

  public void setType(LicenceType type) {
    this.type = type;
  }

  public LicenceSubtype getSubtype() {
    return subtype;
  }

  public void setSubtype(LicenceSubtype subtype) {
    this.subtype = subtype;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getLicenceNumber() {
    return licenceNumber;
  }

  public void setLicenceNumber(String licenceNumber) {
    this.licenceNumber = licenceNumber;
  }

  public String getLicenceReference() {
    return licenceReference;
  }

  public void setLicenceReference(String licenceReference) {
    this.licenceReference = licenceReference;
  }

  public String getRoundIssuedOn() {
    return roundIssuedOn;
  }

  public void setRoundIssuedOn(String roundNumber) {
    this.roundIssuedOn = roundNumber;
  }

  public LicenceStatus getStatus() {
    return status;
  }

  public void setStatus(LicenceStatus status) {
    this.status = status;
  }

  public LicenceTeam getResponsibleTeam() {
    return responsibleTeam;
  }

  public void setResponsibleTeam(LicenceTeam responsibleTeam) {
    this.responsibleTeam = responsibleTeam;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Licence licence = (Licence) o;
    return Objects.equals(id, licence.id)
        && type == licence.type
        && subtype == licence.subtype
        && Objects.equals(prefix, licence.prefix)
        && Objects.equals(licenceNumber, licence.licenceNumber)
        && Objects.equals(licenceReference, licence.licenceReference)
        && Objects.equals(roundIssuedOn, licence.roundIssuedOn)
        && Objects.equals(responsibleTeam, licence.responsibleTeam);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, subtype, prefix, licenceNumber, licenceReference, roundIssuedOn, responsibleTeam);
  }
}

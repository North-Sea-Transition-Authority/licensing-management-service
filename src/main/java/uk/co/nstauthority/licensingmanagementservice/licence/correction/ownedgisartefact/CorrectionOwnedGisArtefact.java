package uk.co.nstauthority.licensingmanagementservice.licence.correction.ownedgisartefact;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;

@Audited
@Entity(name = "licence_correction_owned_gis_artefacts")
public class CorrectionOwnedGisArtefact {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_correction_id")
  private LicenceCorrection licenceCorrection;

  @Enumerated(EnumType.STRING)
  private CorrectionOwnedGisArtefactType artefactType;

  private UUID artefactId;

  public UUID getId() {
    return id;
  }

  public LicenceCorrection getLicenceCorrection() {
    return licenceCorrection;
  }

  public void setLicenceCorrection(LicenceCorrection licenceCorrection) {
    this.licenceCorrection = licenceCorrection;
  }

  public CorrectionOwnedGisArtefactType getArtefactType() {
    return artefactType;
  }

  public void setArtefactType(CorrectionOwnedGisArtefactType artefactType) {
    this.artefactType = artefactType;
  }

  public UUID getArtefactId() {
    return artefactId;
  }

  public void setArtefactId(UUID artefactId) {
    this.artefactId = artefactId;
  }
}

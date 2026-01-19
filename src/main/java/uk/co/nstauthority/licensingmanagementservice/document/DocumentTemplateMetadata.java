package uk.co.nstauthority.licensingmanagementservice.document;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@Entity
@Audited
@Table(name = "document_templates_metadata")
public class DocumentTemplateMetadata {

  @Id
  @UuidGenerator
  private UUID id;

  private UUID documentTemplateId;

  @Enumerated(EnumType.STRING)
  private LicenceType licenceType;

  @Enumerated(EnumType.STRING)
  private ApplicationType applicationType;

  public DocumentTemplateMetadata() {
  }

  @VisibleForTesting
  public DocumentTemplateMetadata(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public UUID getDocumentTemplateId() {
    return documentTemplateId;
  }

  public void setDocumentTemplateId(UUID documentTemplateId) {
    this.documentTemplateId = documentTemplateId;
  }

  public LicenceType getLicenceType() {
    return licenceType;
  }

  public void setLicenceType(LicenceType licenceType) {
    this.licenceType = licenceType;
  }

  public ApplicationType getApplicationType() {
    return applicationType;
  }

  public void setApplicationType(ApplicationType applicationType) {
    this.applicationType = applicationType;
  }
}
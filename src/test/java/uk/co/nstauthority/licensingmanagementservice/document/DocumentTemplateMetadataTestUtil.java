package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

public class DocumentTemplateMetadataTestUtil {

  private DocumentTemplateMetadataTestUtil() {}

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private UUID documentTemplateId = UUID.randomUUID();
    private LicenceType licenceType = LicenceType.SEAWARD_PRODUCTION;
    private ApplicationType applicationType = ApplicationType.CONTINUATION_APPLICATION;

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withDocumentTemplateId(UUID documentTemplateId) {
      this.documentTemplateId = documentTemplateId;
      return this;
    }

    public Builder withLicenceType(LicenceType licenceType) {
      this.licenceType = licenceType;
      return this;
    }

    public Builder withApplicationType(ApplicationType applicationType) {
      this.applicationType = applicationType;
      return this;
    }

    public DocumentTemplateMetadata build() {
      var docTemplateMetadata = new DocumentTemplateMetadata(id);
      docTemplateMetadata.setDocumentTemplateId(this.documentTemplateId);
      docTemplateMetadata.setLicenceType(this.licenceType);
      docTemplateMetadata.setApplicationType(this.applicationType);
      return docTemplateMetadata;
    }
  }
}

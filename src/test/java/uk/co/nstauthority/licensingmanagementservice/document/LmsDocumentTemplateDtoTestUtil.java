package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

public class LmsDocumentTemplateDtoTestUtil {
  private LmsDocumentTemplateDtoTestUtil() {
    throw new IllegalStateException("Cannot instantiate static util class");
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID documentTemplateId = UUID.randomUUID();
    private String title = "title";
    private String description = "description";
    private LicenceType licenceType = LicenceType.SEAWARD_PRODUCTION;
    private ApplicationType applicationType = ApplicationType.CONTINUATION_APPLICATION;
    private String documentTemplateUrl = "url";

    public Builder withDocumentTemplateId(UUID documentTemplateId) {
      this.documentTemplateId = documentTemplateId;
      return this;
    }

    public Builder withTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withLicenceType(LicenceType groupType) {
      this.licenceType = groupType;
      return this;
    }

    public Builder withApplicationType(ApplicationType energyType) {
      this.applicationType = energyType;
      return this;
    }

    public Builder withDocumentTemplateUrl(String documentTemplateUrl) {
      this.documentTemplateUrl = documentTemplateUrl;
      return this;
    }

    public LmsDocumentTemplateDto build() {
      return new LmsDocumentTemplateDto(
          this.documentTemplateId,
          this.title,
          this.description,
          this.licenceType,
          this.applicationType,
          0,
          this.documentTemplateUrl
      );
    }
  }
}

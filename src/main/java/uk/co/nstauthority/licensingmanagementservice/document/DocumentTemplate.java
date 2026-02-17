package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.ArrayList;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

public class DocumentTemplate {

  private DocumentTemplateType documentTemplateType;
  private List<DocumentTemplateSection> documentSections;
  private LicenceType licenceType;
  private ApplicationType applicationType;
  private int displayOrder;
  private String mnemonic;

  public DocumentTemplateType getType() {
    return documentTemplateType;
  }

  public List<DocumentTemplateSection> getDocumentSections() {
    return documentSections;
  }

  public LicenceType getLicenceType() {
    return licenceType;
  }

  public ApplicationType getApplicationType() {
    return applicationType;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public String getMnemonic() {
    return mnemonic;
  }

  public DocumentTemplate(
      DocumentTemplateType documentTemplateType,
      List<DocumentTemplateSection> documentSections,
      LicenceType licenceType,
      ApplicationType applicationType,
      int displayOrder,
      String mnemonic
  ) {
    this.documentTemplateType = documentTemplateType;
    this.documentSections = documentSections;
    this.licenceType = licenceType;
    this.applicationType = applicationType;
    this.displayOrder = displayOrder;
    this.mnemonic = mnemonic;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private DocumentTemplateType documentTemplateType;
    private List<DocumentTemplateSection> documentSections = new ArrayList<>();
    private LicenceType licenceType = null;
    private ApplicationType applicationType = null;
    private int displayOrder;

    public Builder withTemplate(DocumentTemplateType documentTemplate) {
      this.documentTemplateType = documentTemplate;
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

    public Builder withDisplayOrder(int displayOrder) {
      this.displayOrder = displayOrder;
      return this;
    }

    public DocumentTemplateSection.Builder withSection(String sectionTitle) {
      return DocumentTemplateSection.newBuilder(sectionTitle, this);
    }

    // This method should never be called in a consumer, it should only be used in the DocumentTemplateSection class
    // to add the new section to our list of sections
    Builder withSection(DocumentTemplateSection section) {
      documentSections.add(section);
      return this;
    }

    public DocumentTemplate build() {
      var mnemonic = (applicationType != null)
          ? "%s-%s".formatted(documentTemplateType.name(), applicationType.name())
          : documentTemplateType.name();

      return new DocumentTemplate(documentTemplateType, documentSections, licenceType, applicationType, displayOrder, mnemonic);
    }
  }
}

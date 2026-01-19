package uk.co.nstauthority.licensingmanagementservice.document.search;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class DocumentTemplateSearchFilterForm implements Serializable {

  @Serial
  private static final long serialVersionUID = 155413159338411457L;

  private String documentTemplateTitle;

  private List<String> licenceTypes;

  public String getDocumentTemplateTitle() {
    return documentTemplateTitle;
  }

  public void setDocumentTemplateTitle(String documentTemplateTitle) {
    this.documentTemplateTitle = documentTemplateTitle;
  }

  public List<String> getLicenceTypes() {
    return licenceTypes;
  }

  public void setLicenceTypes(List<String> licenceTypes) {
    this.licenceTypes = licenceTypes;
  }

  public void clearFilters() {
    setLicenceTypes(null);
    setDocumentTemplateTitle(null);
  }
}
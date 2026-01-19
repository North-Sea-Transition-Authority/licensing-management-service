package uk.co.nstauthority.licensingmanagementservice.document.search;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.web.bind.annotation.SessionAttributes;

@SessionAttributes("documentTemplateSearchSession")
public class DocumentTemplateSearchSession implements Serializable {

  @Serial
  private static final long serialVersionUID = 38746298374L;

  private DocumentTemplateSearchFilterForm searchFilterForm;

  public DocumentTemplateSearchSession(DocumentTemplateSearchFilterForm documentTemplateSearchFilterForm) {
    this.searchFilterForm = documentTemplateSearchFilterForm;
  }

  public void clearFilters() {
    searchFilterForm.clearFilters();
  }

  public void update(DocumentTemplateSearchFilterForm documentTemplateSearchFilterForm) {
    this.searchFilterForm = documentTemplateSearchFilterForm;
  }

  public DocumentTemplateSearchFilterForm getSearchFilterForm() {
    return searchFilterForm;
  }
}

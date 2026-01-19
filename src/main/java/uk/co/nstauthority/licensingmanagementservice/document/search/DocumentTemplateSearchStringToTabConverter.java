package uk.co.nstauthority.licensingmanagementservice.document.search;

import java.util.EnumSet;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DocumentTemplateSearchStringToTabConverter implements Converter<String, DocumentTemplateSearchTab> {

  static final DocumentTemplateSearchTab DEFAULT_TAB = DocumentTemplateSearchTab.CONTINUATION;

  @Override
  public DocumentTemplateSearchTab convert(String tab) {
    return EnumSet.allOf(DocumentTemplateSearchTab.class)
        .stream()
        .filter(viewDocumentTemplatesTab -> viewDocumentTemplatesTab.getAnchor().equals(tab)
            || viewDocumentTemplatesTab.name().equals(tab))
        .findFirst()
        .orElse(DEFAULT_TAB);
  }
}

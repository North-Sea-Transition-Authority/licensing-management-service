package uk.co.nstauthority.licensingmanagementservice.document.search;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DocumentTemplateSearchTabToStringConverter implements Converter<DocumentTemplateSearchTab, String> {

  @Override
  public String convert(DocumentTemplateSearchTab tab) {
    return tab.getAnchor();
  }
}

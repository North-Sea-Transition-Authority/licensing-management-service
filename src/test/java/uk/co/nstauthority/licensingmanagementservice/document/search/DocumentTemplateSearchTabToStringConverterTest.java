package uk.co.nstauthority.licensingmanagementservice.document.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DocumentTemplateSearchTabToStringConverterTest {

  private final DocumentTemplateSearchTabToStringConverter converter = new DocumentTemplateSearchTabToStringConverter();

  @ParameterizedTest
  @EnumSource(DocumentTemplateSearchTab.class)
  void convert(DocumentTemplateSearchTab tab) {
    assertThat(converter.convert(tab)).isEqualTo(tab.getAnchor());
  }

}
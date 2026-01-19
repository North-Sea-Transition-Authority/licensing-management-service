package uk.co.nstauthority.licensingmanagementservice.document.search;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.nstauthority.licensingmanagementservice.document.search.DocumentTemplateSearchStringToTabConverter.DEFAULT_TAB;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentTemplateSearchStringToTabConverterTest {

  @InjectMocks
  private DocumentTemplateSearchStringToTabConverter converter;

  @ParameterizedTest
  @EnumSource(DocumentTemplateSearchTab.class)
  void convert_whenTabAnchorExists_thenReturnTabEnum(DocumentTemplateSearchTab tab) {
    assertThat(converter.convert(tab.getAnchor())).isEqualTo(tab);
  }

  @ParameterizedTest
  @EnumSource(DocumentTemplateSearchTab.class)
  void convert_whenUsingTabName_thenReturnTabEnum(DocumentTemplateSearchTab tab) {
    assertThat(converter.convert(tab.name())).isEqualTo(tab);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"not a tab"})
  void convert_whenTabAnchorDoesNotExists_thenReturnDefaultTabEnum(String tab) {
    assertThat(converter.convert(tab)).isEqualTo(DEFAULT_TAB);
  }
}
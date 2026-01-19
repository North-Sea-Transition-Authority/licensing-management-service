package uk.co.nstauthority.licensingmanagementservice.document.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

class DocumentTemplateFilterUtilsTest {

  @Test
  void filterLicenceType_whenValid_thenTrue() {
    var lmsDocumentTemplateDto = LmsDocumentTemplateDtoTestUtil.newBuilder().withLicenceType(LicenceType.SEAWARD_EXPLORATION).build();
    assertThat(DocumentTemplateFilterUtils.filterLicenceType(lmsDocumentTemplateDto, List.of(LicenceType.SEAWARD_EXPLORATION.getEnumName())))
        .isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void filterLicenceType_whenNullOrEmpty_thenTrue(List<String> groupType) {
    var lmsDocumentTemplateDto = LmsDocumentTemplateDtoTestUtil.newBuilder().build();
    assertThat(DocumentTemplateFilterUtils.filterLicenceType(lmsDocumentTemplateDto, groupType))
        .isTrue();
  }

  @Test
  void filterLicenceType_whenInvalidEnum_thenTrue() {
    var lmsDocumentTemplateDto = LmsDocumentTemplateDtoTestUtil.newBuilder().build();
    assertThat(DocumentTemplateFilterUtils.filterLicenceType(lmsDocumentTemplateDto, List.of("invalid")))
        .isTrue();
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, names = "SEAWARD_PRODUCTION", mode = EnumSource.Mode.EXCLUDE)
  void filterLicenceType_whenDifferentEnum_thenFalse(LicenceType groupType) {
    var lmsDocumentTemplateDto = LmsDocumentTemplateDtoTestUtil.newBuilder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    assertThat(DocumentTemplateFilterUtils.filterLicenceType(lmsDocumentTemplateDto, List.of(groupType.getEnumName())))
        .isFalse();
  }

  @Test
  void filerDocumentTitle_whenMatching_thenTrue() {
    var lmsDocumentTemplateDto = LmsDocumentTemplateDtoTestUtil.newBuilder().withTitle("test").build();
    assertThat(DocumentTemplateFilterUtils.filterDocumentTitle(lmsDocumentTemplateDto, "ES")).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void filterDocumentTitle_whenNullOrEmpty_thenTrue(String title) {
    var lmsDocumentTemplateDto = LmsDocumentTemplateDtoTestUtil.newBuilder().withTitle("test").build();
    assertThat(DocumentTemplateFilterUtils.filterDocumentTitle(lmsDocumentTemplateDto, title)).isTrue();
  }

  @Test
  void filterDocumentTitle_whenNotMatching_thenFalse() {
    var lmsDocumentTemplateDto = LmsDocumentTemplateDtoTestUtil.newBuilder().withTitle("test").build();
    assertThat(DocumentTemplateFilterUtils.filterDocumentTitle(lmsDocumentTemplateDto, "invalid")).isFalse();
  }
}
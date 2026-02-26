package uk.co.nstauthority.licensingmanagementservice.document;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption.ADD_AFTER;
import static uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption.ADD_BEFORE;
import static uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption.ADD_SUBSECTION;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DocumentSectionUtilTest {

  @ParameterizedTest
  @MethodSource("getPageTitleToEnum")
  void getAddSectionPageTitle(
      AddSectionOption addSectionOption,
      String pageTitle
  ) {
    var title = "title";
    assertThat(DocumentSectionUtil.getAddSectionPageTitle(title, addSectionOption)).isEqualTo(pageTitle.formatted(title));
  }

  private static Stream<Arguments> getPageTitleToEnum() {
    return Stream.of(
        Arguments.of(ADD_BEFORE, "Add section before %s"),
        Arguments.of(ADD_AFTER, "Add section after %s"),
        Arguments.of(ADD_SUBSECTION, "Add subsection")
    );
  }
}
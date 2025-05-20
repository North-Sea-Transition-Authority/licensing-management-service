package uk.co.nstauthority.template.xyzapplication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class XyzApplicationFilterServiceTest {

  @InjectMocks
  private XyzApplicationFilterService xyzApplicationFilterService;

  @ParameterizedTest
  @MethodSource("getDifferentStringsContainingFilterTerm")
  void filterReferences_contains_true(String reference) {
    var application = new XyzApplication();
    application.setReference(reference);

    assertThat(xyzApplicationFilterService.filterReference(application, "test String"))
        .isTrue();
  }

  public static Stream<Arguments> getDifferentStringsContainingFilterTerm() {
    return Stream.of(
        Arguments.of("test String"),
        Arguments.of("test string"),
        Arguments.of("TEST STRING"),
        Arguments.of("tEsT sTrInG"),
        Arguments.of("ANOTHER TEST string to be matched")
    );
  }

  @Test
  void filterReferences_filterByBlankReference_true() {
    var application = new XyzApplication();
    application.setReference("test String");

    assertThat(xyzApplicationFilterService.filterReference(application, ""))
        .isTrue();
  }

  @Test
  void filterReferences_filterByNullReference_true() {
    var application = new XyzApplication();
    application.setReference("test String");

    assertThat(xyzApplicationFilterService.filterReference(application, null))
        .isTrue();
  }

  @Test
  void filterReferences_applicationWithNullReference_false() {
    var application = new XyzApplication();

    assertThat(xyzApplicationFilterService.filterReference(application, "test String"))
        .isFalse();
  }

  @Test
  void filterReferences_applicationWithBlankReference_false() {
    var application = new XyzApplication();
    application.setReference("");

    assertThat(xyzApplicationFilterService.filterReference(application, "test String"))
        .isFalse();
  }

  @ParameterizedTest
  @MethodSource("getDifferentStringsNotContainingFilterTerm")
  void filterReferences_doesNotContain_false(String reference) {
    var application = new XyzApplication();
    application.setReference(reference);

    assertThat(xyzApplicationFilterService.filterReference(application, "test String"))
        .isFalse();
  }

  public static Stream<Arguments> getDifferentStringsNotContainingFilterTerm() {
    return Stream.of(
        Arguments.of("apple"),
        Arguments.of("test-string"),
        Arguments.of("testString"),
        Arguments.of("test_String"),
        Arguments.of("ANOTHER TESTING string to be matched")
    );
  }
}

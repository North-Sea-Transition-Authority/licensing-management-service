package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApplicationReferenceUtilTest {

  @Test
  void withVersionNumber_whenReferenceHasNoSuffix_appendsVersionNumber() {
    assertThat(ApplicationReferenceUtil.withVersionNumber("LMS/EAA/2024/5", 2)).isEqualTo("LMS/EAA/2024/5/2");
  }

  @Test
  void withVersionNumber_whenReferenceHasSuffix_replacesSuffix() {
    assertThat(ApplicationReferenceUtil.withVersionNumber("LMS/CA/2024/5/2", 3)).isEqualTo("LMS/CA/2024/5/3");
  }
}

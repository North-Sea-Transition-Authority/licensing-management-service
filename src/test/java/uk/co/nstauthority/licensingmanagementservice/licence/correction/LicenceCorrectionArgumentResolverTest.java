package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class LicenceCorrectionArgumentResolverTest {

  private final LicenceCorrectionArgumentResolver resolver = new LicenceCorrectionArgumentResolver();

  @Test
  void resolveArgument_whenValidatedCorrectionOnRequest_thenReturnsIt() {
    var correction = LicenceCorrectionTestUtil.newBuilder().build();
    var request = new MockHttpServletRequest();
    request.setAttribute("validatedCorrection", correction);

    var result = resolver.resolveArgument(null, null, new ServletWebRequest(request), null);

    assertThat(result).isSameAs(correction);
  }

  @Test
  void resolveArgument_whenNoValidatedCorrectionOnRequest_thenThrows() {
    var webRequest = new ServletWebRequest(new MockHttpServletRequest());

    assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
        .isInstanceOf(IllegalStateException.class);
  }

}
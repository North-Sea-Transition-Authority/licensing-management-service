package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InterceptorRuleTestEndpoints;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;

class CorrectionLicenceIsTypeRuleTest extends AbstractInterceptorRuleTest {

  @InjectMocks
  private CorrectionLicenceIsTypeRule rule;

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(CorrectionLicenceIsType.class);
  }

  @Test
  void check_whenLicenceIsExpectedType_continueAsNormal() throws NoSuchMethodException {
    when(request.getAttribute("validatedCorrection"))
        .thenReturn(correctionWithLicenceType(LicenceType.CARBON_STORAGE));

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("correctionLicenceIsType"),
        CorrectionLicenceIsType.class
    );

    var result = rule.check(annotation, request, response);

    assertThat(result.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_whenLicenceIsOneOfMultipleExpectedTypes_continueAsNormal() throws NoSuchMethodException {
    when(request.getAttribute("validatedCorrection"))
        .thenReturn(correctionWithLicenceType(LicenceType.CARBON_STORAGE));

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("correctionLicenceIsType_multipleTypes"),
        CorrectionLicenceIsType.class
    );

    var result = rule.check(annotation, request, response);

    assertThat(result.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_whenLicenceIsNotExpectedType_forbidden() throws NoSuchMethodException {
    when(request.getAttribute("validatedCorrection"))
        .thenReturn(correctionWithLicenceType(LicenceType.GAS_STORAGE));

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("correctionLicenceIsType"),
        CorrectionLicenceIsType.class
    );

    var result = rule.check(annotation, request, response);

    assertThat(result).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN
    );
  }

  @Test
  void check_whenNoLicenceTypesProvided_exception() throws NoSuchMethodException {
    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("correctionLicenceIsType_noProvidedTypes"),
        CorrectionLicenceIsType.class
    );

    assertThatThrownBy(() -> rule.check(annotation, request, response))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("No licence types provided to security annotation")
        .matches(e -> ((ResponseStatusException) e).getStatusCode().is5xxServerError());
  }

  private LicenceCorrection correctionWithLicenceType(LicenceType licenceType) {
    var licence = LicenceTestUtil.builder()
        .withLicenceType(licenceType)
        .build();
    return LicenceCorrectionTestUtil.newBuilder()
        .withLicence(licence)
        .build();
  }
}
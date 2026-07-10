package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanRemoveLicencePosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanRemoveLicencePositionInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

class InvokingUserCanRemoveLicencePositionInterceptorRuleTest extends AbstractInterceptorRuleTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionService licencePositionService;

  @InjectMocks
  private InvokingUserCanRemoveLicencePositionInterceptorRule invokingUserCanRemoveLicencePositionInterceptorRule;

  @Test
  void supports() {
    assertThat(invokingUserCanRemoveLicencePositionInterceptorRule.supports())
        .isEqualTo(InvokingUserCanRemoveLicencePosition.class);
  }

  @Test
  void check_whenPositionRemovable_rulePasses() throws NoSuchMethodException {
    var position = mockCorrectionAndPosition();
    when(licencePositionCorrectionService.canRemovePosition(any(LicenceCorrection.class), eq(position)))
        .thenReturn(true);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("canRemoveLicencePosition"),
        InvokingUserCanRemoveLicencePosition.class
    );

    var result = invokingUserCanRemoveLicencePositionInterceptorRule.check(annotation, request, response);

    assertThat(result.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_whenPositionNotRemovable_ruleFailsForbidden() throws NoSuchMethodException {
    var position = mockCorrectionAndPosition();
    when(licencePositionCorrectionService.canRemovePosition(any(LicenceCorrection.class), eq(position)))
        .thenReturn(false);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("canRemoveLicencePosition"),
        InvokingUserCanRemoveLicencePosition.class
    );

    var result = invokingUserCanRemoveLicencePositionInterceptorRule.check(annotation, request, response);

    assertThat(result).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN
    );
  }

  private LicencePosition mockCorrectionAndPosition() {
    var positionId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var position = LicencePositionTestUtil.newBuilder().withId(positionId).withLicence(LICENCE).build();

    when(request.getAttribute("validatedCorrection")).thenReturn(correction);
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("licencePositionId", positionId.toString()));
    when(licencePositionService.getPositionForLicence(LICENCE, positionId)).thenReturn(position);

    return position;
  }
}
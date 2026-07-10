package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InterceptorRuleTestEndpoints;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;

@ExtendWith(MockitoExtension.class)
class InvokingUserCanViewCorrectionInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @Mock
  private UserDetailService userDetailService;

  @InjectMocks
  private InvokingUserCanViewCorrectionInterceptorRule rule;

  private ServiceUserDetail user = ServiceUserDetailTestUtil.newBuilder().build();

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(InvokingUserCanViewCorrection.class);
  }

  @Test
  void check_userCanViewCorrection_continueAsNormal() throws NoSuchMethodException {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
        .build();

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("correctionId", correction.getId().toString()));
    when(userDetailService.getUserDetail()).thenReturn(user);
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(correction.getId(), user))
        .thenReturn(Optional.of(correction));

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("invokingUserCanViewCorrection", UUID.class),
        InvokingUserCanViewCorrection.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_userUserIsNotAssignedToCorrection_forbidden() throws NoSuchMethodException {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
        .build();

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("correctionId", correction.getId().toString()));
    when(userDetailService.getUserDetail()).thenReturn(user);
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(correction.getId(), user))
        .thenReturn(Optional.empty());

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("invokingUserCanViewCorrection", UUID.class),
        InvokingUserCanViewCorrection.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN
    );
  }

  @Test
  void check_correctionIsCancelled_forbidden() throws NoSuchMethodException {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withStatus(LicenceCorrectionStatus.CANCELLED)
        .build();

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("correctionId", correction.getId().toString()));
    when(userDetailService.getUserDetail()).thenReturn(user);
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(correction.getId(), user))
        .thenReturn(Optional.of(correction));

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("invokingUserCanViewCorrection", UUID.class),
        InvokingUserCanViewCorrection.class
    );

    var interceptorResult = rule.check(annotation, request, response);

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN
    );
  }
}
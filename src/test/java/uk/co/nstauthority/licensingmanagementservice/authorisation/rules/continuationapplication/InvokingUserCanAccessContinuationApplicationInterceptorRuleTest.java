package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class InvokingUserCanAccessContinuationApplicationInterceptorRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private LicenceContinuationService licenceContinuationService;
  @Mock
  private ApplicationAccessService applicationAccessService;
  @Mock
  private UserDetailService userDetailService;
  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @InjectMocks
  private InvokingUserCanAccessContinuationApplicationInterceptorRule invokingUserCanAccessContinuationApplicationInterceptorRule;

  @Test
  void supports() {
    assertThat(invokingUserCanAccessContinuationApplicationInterceptorRule.supports())
        .isEqualTo(InvokingUserCanAccessContinuationApplication.class);
  }

  @Test
  void check_userHasAccess_rulePass() throws NoSuchMethodException {
    var detailId = UUID.randomUUID();
    var wuaId = 123L;

    mockUserAndApplication(detailId, wuaId);
    when(applicationAccessService.userHasAccessToApplication(
        any(LicenceApplicationDetail.class), any(), eq(wuaId)))
        .thenReturn(true);

    var annotation = getAnnotation(
        InvokingUserCanAccessContinuationApplicationInterceptorRuleTest.class.getDeclaredMethod("accessControlledEndpoint"),
        InvokingUserCanAccessContinuationApplication.class
    );

    var interceptorResult = invokingUserCanAccessContinuationApplicationInterceptorRule.check(annotation, request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_userDoesNotHaveAccess_ruleFail() throws NoSuchMethodException {
    var detailId = UUID.randomUUID();
    var wuaId = 123L;

    mockUserAndApplication(detailId, wuaId);
    when(applicationAccessService.userHasAccessToApplication(
        any(LicenceApplicationDetail.class), any(), eq(wuaId)))
        .thenReturn(false);

    var annotation = getAnnotation(
        InvokingUserCanAccessContinuationApplicationInterceptorRuleTest.class.getDeclaredMethod("accessControlledEndpoint"),
        InvokingUserCanAccessContinuationApplication.class
    );

    var interceptorResult = invokingUserCanAccessContinuationApplicationInterceptorRule.check(annotation, request, response);

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN
    );
  }

  private void mockUserAndApplication(UUID detailId,
                                      Long wuaId) {
    var userDetail = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(wuaId)
        .build();
    when(userDetailService.getUserDetail()).thenReturn(userDetail);

    var licence = LicenceTestUtil
        .builder()
        .withId(1)
        .build();

    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence));

    var licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil
        .createLicenceContinuationApplicationDetail(licenceScheduleDetail);
    licenceContinuationApplicationDetail.setId(detailId);

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(
        Map.of(LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID, detailId.toString())
    );

    when(licenceContinuationService.getDetailByIdOrThrow(detailId))
        .thenReturn(licenceContinuationApplicationDetail);

  }

  @GetMapping("access-controlled-endpoint")
  @InvokingUserCanAccessContinuationApplication
  public ResponseEntity<String> accessControlledEndpoint() {
    return ResponseEntity.ok("Access granted");
  }
}
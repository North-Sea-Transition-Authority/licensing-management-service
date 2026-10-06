package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InterceptorRuleTestEndpoints;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

class InvokingUserCanReinstateLicencePositionInterceptorRuleTest extends AbstractInterceptorRuleTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicencePositionService licencePositionService;

  @InjectMocks
  private LicencePositionCanBeReinstantiatedRule licencePositionCanBeReinstantiatedRule;

  @Test
  void supports() {
    assertThat(licencePositionCanBeReinstantiatedRule.supports())
        .isEqualTo(LicencePositionCanBeReinstantiated.class);
  }

  @Test
  void check_whenPositionReinstatable_rulePasses() throws NoSuchMethodException {
    var position = mockCorrectionAndPosition();
    when(licencePositionCorrectionService.canReinstateDeletedPositionCorrection(any(LicenceCorrection.class), eq(position)))
        .thenReturn(true);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("canReinstateLicencePosition"),
        LicencePositionCanBeReinstantiated.class
    );

    var result = licencePositionCanBeReinstantiatedRule.check(annotation, request, response);

    assertThat(result.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_whenPositionNotReinstatable_ruleFailsForbidden() throws NoSuchMethodException {
    var position = mockCorrectionAndPosition();
    when(licencePositionCorrectionService.canReinstateDeletedPositionCorrection(any(LicenceCorrection.class), eq(position)))
        .thenReturn(false);

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("canReinstateLicencePosition"),
        LicencePositionCanBeReinstantiated.class
    );

    var result = licencePositionCanBeReinstantiatedRule.check(annotation, request, response);

    assertThat(result).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.FORBIDDEN
    );
  }

  @Test
  void check_whenCorrectionNotFound_thenNotFound() throws NoSuchMethodException {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(
            "correctionId", CORRECTION.getId().toString(),
            "licencePositionId", POSITION_ID.toString()
        ));
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.empty());

    var annotation = getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("canReinstateLicencePosition"),
        LicencePositionCanBeReinstantiated.class
    );

    var result = licencePositionCanBeReinstantiatedRule.check(annotation, request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "Licence correction %s not found".formatted(CORRECTION.getId())
    ));
    verifyNoInteractions(licencePositionService, licencePositionCorrectionService);
  }

  private LicencePosition mockCorrectionAndPosition() {

    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(
            "correctionId", CORRECTION.getId().toString(),
            "licencePositionId", POSITION_ID.toString()
        ));
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.of(CORRECTION));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);

    return POSITION;
  }

}
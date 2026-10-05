package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction;

import static org.assertj.core.api.Assertions.assertThat;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;

class LicencePositionCorrectionBelongsToCorrectionRuleTest extends AbstractInterceptorRuleTest {

  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @InjectMocks
  private LicencePositionCorrectionBelongsToCorrectionRule rule;

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(LicencePositionCorrectionBelongsToCorrection.class);
  }

  @Test
  void check_whenPositionCorrectionBelongsToCorrection_thenContinueAsNormal() throws NoSuchMethodException {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(CORRECTION)
        .build();

    stubRequest();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(positionCorrection));
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.of(CORRECTION));

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.continueAsNormal());
    verifyNoInteractions(response);
  }

  @Test
  void check_whenPositionCorrectionNotFound_thenNotFound() throws NoSuchMethodException {
    stubRequest();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.empty());
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.of(CORRECTION));

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "No licence position correction %s".formatted(POSITION_CORRECTION_ID)
    ));
  }

  @Test
  void check_whenCorrectionNotFound_thenNotFound() throws NoSuchMethodException {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(CORRECTION)
        .build();

    stubRequest();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(positionCorrection));
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.empty());

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "No licence position correction %s".formatted(POSITION_CORRECTION_ID)
    ));
  }

  @Test
  void check_whenPositionCorrectionBelongsToAnotherCorrection_thenNotFound() throws NoSuchMethodException {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(LicenceCorrectionTestUtil.newBuilder().build())
        .build();

    stubRequest();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(positionCorrection));
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.of(CORRECTION));

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "Licence position correction %s does not belong to correction %s".formatted(
            POSITION_CORRECTION_ID,
            CORRECTION.getId())
    ));
  }

  private void stubRequest() {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(
            "licencePositionCorrectionId", POSITION_CORRECTION_ID.toString(),
            "correctionId", CORRECTION.getId().toString()
        ));
  }

  private LicencePositionCorrectionBelongsToCorrection annotation() throws NoSuchMethodException {
    return getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("licencePositionCorrectionBelongsToCorrection"),
        LicencePositionCorrectionBelongsToCorrection.class
    );
  }
}

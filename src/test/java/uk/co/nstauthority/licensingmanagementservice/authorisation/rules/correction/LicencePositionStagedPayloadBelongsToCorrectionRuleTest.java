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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadTestUtil;

class LicencePositionStagedPayloadBelongsToCorrectionRuleTest extends AbstractInterceptorRuleTest {

  private static final UUID POSITION_STAGED_PAYLOAD_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();

  @Mock
  private LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @InjectMocks
  private LicencePositionStagedPayloadBelongsToCorrectionRule rule;

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(LicencePositionStagedPayloadBelongsToCorrection.class);
  }

  @Test
  void check_whenPositionStagedPayloadBelongsToCorrection_thenContinueAsNormal() throws NoSuchMethodException {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(POSITION_STAGED_PAYLOAD_ID)
        .withLicenceCorrection(CORRECTION)
        .build();

    stubRequest();
    when(licencePositionStagedPayloadService.findById(POSITION_STAGED_PAYLOAD_ID)).thenReturn(Optional.of(positionStagedPayload));
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.of(CORRECTION));

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.continueAsNormal());
    verifyNoInteractions(response);
  }

  @Test
  void check_whenPositionStagedPayloadNotFound_thenNotFound() throws NoSuchMethodException {
    stubRequest();
    when(licencePositionStagedPayloadService.findById(POSITION_STAGED_PAYLOAD_ID)).thenReturn(Optional.empty());
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.of(CORRECTION));

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "No licence position staged payload %s".formatted(POSITION_STAGED_PAYLOAD_ID)
    ));
  }

  @Test
  void check_whenCorrectionNotFound_thenNotFound() throws NoSuchMethodException {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(POSITION_STAGED_PAYLOAD_ID)
        .withLicenceCorrection(CORRECTION)
        .build();

    stubRequest();
    when(licencePositionStagedPayloadService.findById(POSITION_STAGED_PAYLOAD_ID)).thenReturn(Optional.of(positionStagedPayload));
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.empty());

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "No licence position staged payload %s".formatted(POSITION_STAGED_PAYLOAD_ID)
    ));
  }

  @Test
  void check_whenPositionStagedPayloadBelongsToAnotherCorrection_thenNotFound() throws NoSuchMethodException {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(POSITION_STAGED_PAYLOAD_ID)
        .withLicenceCorrection(LicenceCorrectionTestUtil.newBuilder().build())
        .build();

    stubRequest();
    when(licencePositionStagedPayloadService.findById(POSITION_STAGED_PAYLOAD_ID)).thenReturn(Optional.of(positionStagedPayload));
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.of(CORRECTION));

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "Licence position staged payload %s does not belong to correction %s".formatted(
            POSITION_STAGED_PAYLOAD_ID,
            CORRECTION.getId())
    ));
  }

  private void stubRequest() {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(
            "licencePositionStagedPayloadId", POSITION_STAGED_PAYLOAD_ID.toString(),
            "correctionId", CORRECTION.getId().toString()
        ));
  }

  private LicencePositionStagedPayloadBelongsToCorrection annotation() throws NoSuchMethodException {
    return getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("licencePositionStagedPayloadBelongsToCorrection"),
        LicencePositionStagedPayloadBelongsToCorrection.class
    );
  }
}

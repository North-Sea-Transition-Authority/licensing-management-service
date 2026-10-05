package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position;

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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

class LicencePositionBelongsToCorrectionLicenceRuleTest extends AbstractInterceptorRuleTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().withId(100).build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @InjectMocks
  private LicencePositionBelongsToCorrectionLicenceRule rule;

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(LicencePositionBelongsToCorrectionLicence.class);
  }

  @Test
  void check_whenPositionBelongsToCorrectionLicence_thenContinueAsNormal() throws NoSuchMethodException {
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    stubRequest();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.of(CORRECTION));

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.continueAsNormal());
    verifyNoInteractions(response);
  }

  @Test
  void check_whenPositionNotFound_thenNotFound() throws NoSuchMethodException {
    stubRequest();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.empty());
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.of(CORRECTION));

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "No licence position %s".formatted(POSITION_ID)
    ));
  }

  @Test
  void check_whenCorrectionNotFound_thenNotFound() throws NoSuchMethodException {
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    stubRequest();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.empty());

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "No licence position %s".formatted(POSITION_ID)
    ));
  }

  @Test
  void check_whenPositionBelongsToAnotherLicence_thenNotFound() throws NoSuchMethodException {
    var otherLicence = LicenceTestUtil.builder().withId(200).build();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(otherLicence).build();

    stubRequest();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licenceCorrectionService.findById(CORRECTION.getId())).thenReturn(Optional.of(CORRECTION));

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "Licence position %s does not belong to the licence of correction %s".formatted(POSITION_ID, CORRECTION.getId())
    ));
  }

  private void stubRequest() {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(
            "licencePositionId", POSITION_ID.toString(),
            "correctionId", CORRECTION.getId().toString()
        ));
  }

  private LicencePositionBelongsToCorrectionLicence annotation() throws NoSuchMethodException {
    return getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("licencePositionBelongsToCorrectionLicence"),
        LicencePositionBelongsToCorrectionLicence.class
    );
  }
}

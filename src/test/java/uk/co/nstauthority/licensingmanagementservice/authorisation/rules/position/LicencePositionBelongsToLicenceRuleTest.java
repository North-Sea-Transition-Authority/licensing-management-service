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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

class LicencePositionBelongsToLicenceRuleTest extends AbstractInterceptorRuleTest {

  private static final Integer LICENCE_ID = 100;
  private static final UUID POSITION_ID = UUID.randomUUID();

  @Mock
  private LicencePositionService licencePositionService;

  @InjectMocks
  private LicencePositionBelongsToLicenceRule rule;

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(LicencePositionBelongsToLicence.class);
  }

  @Test
  void check_whenPositionBelongsToLicence_thenContinueAsNormal() throws NoSuchMethodException {
    var licence = LicenceTestUtil.builder().withId(LICENCE_ID).build();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(licence).build();

    stubPathVariables();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.continueAsNormal());
    verifyNoInteractions(response);
  }

  @Test
  void check_whenPositionNotFound_thenNotFound() throws NoSuchMethodException {
    stubPathVariables();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.empty());

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

    stubPathVariables();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));

    var result = rule.check(annotation(), request, response);

    assertThat(result).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "Licence position %s does not belong to licence %s".formatted(POSITION_ID, LICENCE_ID)
    ));
  }

  private void stubPathVariables() {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(
            "licenceId", LICENCE_ID.toString(),
            "licencePositionId", POSITION_ID.toString()
        ));
  }

  private LicencePositionBelongsToLicence annotation() throws NoSuchMethodException {
    return getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("licencePositionBelongsToLicence"),
        LicencePositionBelongsToLicence.class
    );
  }
}

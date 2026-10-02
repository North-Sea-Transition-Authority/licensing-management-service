package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.administrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InterceptorRuleTestEndpoints;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@ExtendWith(MockitoExtension.class)
class LicencePositionHasNoLiveChangeOfTypeRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @InjectMocks
  private LicencePositionHasNoLiveChangeOfTypeRule rule;

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(LicencePositionHasNoLiveChangeOfType.class);
  }

  @Test
  void check_whenPositionHasNoLiveChange_continueAsNormal() throws NoSuchMethodException {
    var positionId = UUID.randomUUID();

    stubPathVariables(positionId);
    when(licencePositionChangeService.changeExists(positionId, AdministratorOperation.class)).thenReturn(false);

    var interceptorResult = rule.check(annotation(), request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_whenPositionHasLiveAdministratorChange_conflict() throws NoSuchMethodException {
    var positionId = UUID.randomUUID();

    stubPathVariables(positionId);
    when(licencePositionChangeService.changeExists(positionId, AdministratorOperation.class)).thenReturn(true);

    var interceptorResult = rule.check(annotation(), request, response);

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus,
        SecurityRuleResult::failureMessage
    ).containsExactly(
        false,
        HttpStatus.CONFLICT,
        "Licence position %s already has a live AdministratorOperation".formatted(positionId)
    );
  }

  private void stubPathVariables(UUID licencePositionId) {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("licencePositionId", licencePositionId.toString()));
  }

  private LicencePositionHasNoLiveChangeOfType annotation() throws NoSuchMethodException {
    return getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod("licencePositionHasNoLiveAdministratorChange", UUID.class),
        LicencePositionHasNoLiveChangeOfType.class
    );
  }
}

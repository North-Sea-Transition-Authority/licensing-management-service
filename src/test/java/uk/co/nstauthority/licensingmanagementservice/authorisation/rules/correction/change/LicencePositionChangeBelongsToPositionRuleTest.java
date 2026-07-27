package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AbstractInterceptorRuleTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InterceptorRuleTestEndpoints;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;

@ExtendWith(MockitoExtension.class)
class LicencePositionChangeBelongsToPositionRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @InjectMocks
  private LicencePositionChangeBelongsToPositionRule rule;

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(LicencePositionChangeBelongsToPosition.class);
  }

  @Test
  void check_whenChangeBelongsToPosition_continueAsNormal() throws NoSuchMethodException {
    var positionId = UUID.randomUUID();
    var changeId = UUID.randomUUID();
    var position = LicencePositionTestUtil.newBuilder().withId(positionId).build();
    var change = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withLicencePosition(position)
        .build();

    stubPathVariables(positionId, changeId);
    when(licencePositionChangeService.findById(changeId)).thenReturn(Optional.of(change));

    var interceptorResult = rule.check(annotation(), request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_whenChangeNotFound_notFound() throws NoSuchMethodException {
    var positionId = UUID.randomUUID();
    var changeId = UUID.randomUUID();

    stubPathVariables(positionId, changeId);
    when(licencePositionChangeService.findById(changeId)).thenReturn(Optional.empty());

    var interceptorResult = rule.check(annotation(), request, response);

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.NOT_FOUND
    );
  }

  @Test
  void check_whenChangeBelongsToDifferentPosition_notFound() throws NoSuchMethodException {
    var positionId = UUID.randomUUID();
    var otherPositionId = UUID.randomUUID();
    var changeId = UUID.randomUUID();
    var otherPosition = LicencePositionTestUtil.newBuilder().withId(otherPositionId).build();
    var change = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withLicencePosition(otherPosition)
        .build();

    stubPathVariables(positionId, changeId);
    when(licencePositionChangeService.findById(changeId)).thenReturn(Optional.of(change));

    var interceptorResult = rule.check(annotation(), request, response);

    assertThat(interceptorResult).extracting(
        SecurityRuleResult::hasRulePassed,
        SecurityRuleResult::failureStatus
    ).containsExactly(
        false,
        HttpStatus.NOT_FOUND
    );
  }

  private void stubPathVariables(UUID licencePositionId, UUID changeId) {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of(
            "licencePositionId", licencePositionId.toString(),
            "changeId", changeId.toString()
        ));
  }

  private LicencePositionChangeBelongsToPosition annotation() throws NoSuchMethodException {
    return getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod(
            "licencePositionChangeBelongsToPosition", UUID.class, UUID.class),
        LicencePositionChangeBelongsToPosition.class
    );
  }
}

package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.administrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;

@ExtendWith(MockitoExtension.class)
class ValidLicencePositionAdministratorChangeRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @InjectMocks
  private ValidLicencePositionAdministratorChangeRule rule;

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(ValidLicencePositionAdministratorChange.class);
  }

  @Test
  void check_whenAdministratorChange_continueAsNormal() throws NoSuchMethodException {
    var changeId = UUID.randomUUID();
    var change = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(1).build()))
        .build();

    stubChangeId(changeId);
    when(licencePositionChangeService.findById(changeId)).thenReturn(Optional.of(change));

    var interceptorResult = rule.check(annotation(), request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_whenChangeNotFound_notFound() throws NoSuchMethodException {
    var changeId = UUID.randomUUID();

    stubChangeId(changeId);
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
  void check_whenNotAdministratorChange_notFound() throws NoSuchMethodException {
    var changeId = UUID.randomUUID();
    var change = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withOperations(List.of())
        .build();

    stubChangeId(changeId);
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

  private void stubChangeId(UUID changeId) {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("changeId", changeId.toString()));
  }

  private ValidLicencePositionAdministratorChange annotation() throws NoSuchMethodException {
    return getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod(
            "validLicencePositionAdministratorChange", UUID.class, UUID.class),
        ValidLicencePositionAdministratorChange.class
    );
  }
}

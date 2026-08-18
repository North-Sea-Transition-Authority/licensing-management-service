package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.equity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
class ValidLicencePositionEquityChangeRuleTest extends AbstractInterceptorRuleTest {

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @InjectMocks
  private ValidLicencePositionEquityChangeRule rule;

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(ValidLicencePositionEquityChange.class);
  }

  @Test
  void check_whenSetEquityChange_continueAsNormal() throws NoSuchMethodException {
    var changeId = UUID.randomUUID();
    var change = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withOperations(List.of(
            LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(BigDecimal.TEN).build()))
        .build();

    stubChangeId(changeId);
    when(licencePositionChangeService.findById(changeId)).thenReturn(Optional.of(change));

    var interceptorResult = rule.check(annotation(), request, response);

    assertThat(interceptorResult.hasRulePassed()).isTrue();
    verifyNoInteractions(response);
  }

  @Test
  void check_whenTransferEquityChange_continueAsNormal() throws NoSuchMethodException {
    var changeId = UUID.randomUUID();
    var change = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withOperations(List.of(
            LicenceOperation.newTransferEquityOperation().withTransferFrom(1).withTransferTo(2)
                .withEquity(BigDecimal.TEN).build()))
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
  void check_whenNotEquityChange_notFound() throws NoSuchMethodException {
    var changeId = UUID.randomUUID();
    var change = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(1).build()))
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

  private ValidLicencePositionEquityChange annotation() throws NoSuchMethodException {
    return getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod(
            "validLicencePositionEquityChange", UUID.class, UUID.class),
        ValidLicencePositionEquityChange.class
    );
  }
}
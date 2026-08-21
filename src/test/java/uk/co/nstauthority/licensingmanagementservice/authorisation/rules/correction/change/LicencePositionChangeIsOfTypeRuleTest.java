package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change;

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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;

@ExtendWith(MockitoExtension.class)
class LicencePositionChangeIsOfTypeRuleTest extends AbstractInterceptorRuleTest {

  private static final UUID CHANGE_ID = UUID.randomUUID();

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @InjectMocks
  private LicencePositionChangeIsOfTypeRule rule;

  @Test
  void supports() {
    assertThat(rule.supports()).isEqualTo(LicencePositionChangeIsOfType.class);
  }

  @Test
  void check_whenChangeCarriesAnOperationOfTheAnnotatedType_thenContinueAsNormal() throws NoSuchMethodException {
    givenChangeWithOperations(LicenceOperation.newAdministratorChange().withOperator(1).build());

    var interceptorResult = rule.check(annotation(), request, response);

    assertThat(interceptorResult).isEqualTo(SecurityRuleResult.continueAsNormal());
    verifyNoInteractions(response);
  }

  @Test
  void check_whenChangeNotFound_thenNotFound() throws NoSuchMethodException {
    stubChangeId();
    when(licencePositionChangeService.findById(CHANGE_ID)).thenReturn(Optional.empty());

    var interceptorResult = rule.check(annotation(), request, response);

    assertThat(interceptorResult).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND, "No licence position change %s".formatted(CHANGE_ID)));
  }

  @Test
  void check_whenChangeCarriesNoOperationOfTheAnnotatedType_thenNotFound() throws NoSuchMethodException {
    givenChangeWithOperations(LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(UUID.randomUUID()))
        .build());

    var interceptorResult = rule.check(annotation(), request, response);

    assertThat(interceptorResult).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND, "Change %s does not carry a AdministratorOperation".formatted(CHANGE_ID)));
  }

  @Test
  void check_whenChangeCarriesTheFirstOfMultipleAnnotatedTypes_thenContinueAsNormal() throws NoSuchMethodException {
    givenChangeWithOperations(new SetEquityOperation(1, BigDecimal.TEN));

    var interceptorResult = rule.check(multipleTypesAnnotation(), request, response);

    assertThat(interceptorResult).isEqualTo(SecurityRuleResult.continueAsNormal());
    verifyNoInteractions(response);
  }

  @Test
  void check_whenChangeCarriesTheSecondOfMultipleAnnotatedTypes_thenContinueAsNormal() throws NoSuchMethodException {
    givenChangeWithOperations(new TransferEquityOperation(1, 2, BigDecimal.TEN, null));

    var interceptorResult = rule.check(multipleTypesAnnotation(), request, response);

    assertThat(interceptorResult).isEqualTo(SecurityRuleResult.continueAsNormal());
    verifyNoInteractions(response);
  }

  @Test
  void check_whenChangeNotFoundForMultipleAnnotatedTypes_thenNotFound() throws NoSuchMethodException {
    stubChangeId();
    when(licencePositionChangeService.findById(CHANGE_ID)).thenReturn(Optional.empty());

    var interceptorResult = rule.check(multipleTypesAnnotation(), request, response);

    assertThat(interceptorResult).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND, "No licence position change %s".formatted(CHANGE_ID)));
  }

  @Test
  void check_whenChangeCarriesNoneOfTheMultipleAnnotatedTypes_thenNotFound() throws NoSuchMethodException {
    givenChangeWithOperations(LicenceOperation.newAdministratorChange().withOperator(1).build());

    var interceptorResult = rule.check(multipleTypesAnnotation(), request, response);

    assertThat(interceptorResult).isEqualTo(SecurityRuleResult.checkFailedWithStatusAndMessage(
        HttpStatus.NOT_FOUND,
        "Change %s does not carry a SetEquityOperation or TransferEquityOperation".formatted(CHANGE_ID)));
  }

  private void givenChangeWithOperations(LicenceOperation... operations) {
    stubChangeId();
    when(licencePositionChangeService.findById(CHANGE_ID)).thenReturn(Optional.of(
        LicencePositionChangeTestUtil.newBuilder()
            .withId(CHANGE_ID)
            .withOperations(List.of(operations))
            .build()));
  }

  private void stubChangeId() {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("changeId", CHANGE_ID.toString()));
  }

  private LicencePositionChangeIsOfType annotation() throws NoSuchMethodException {
    return getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod(
            "licencePositionChangeIsOfType", UUID.class, UUID.class),
        LicencePositionChangeIsOfType.class
    );
  }

  private LicencePositionChangeIsOfType multipleTypesAnnotation() throws NoSuchMethodException {
    return getAnnotation(
        InterceptorRuleTestEndpoints.class.getDeclaredMethod(
            "validLicencePositionEquityChange", UUID.class, UUID.class),
        LicencePositionChangeIsOfType.class
    );
  }
}

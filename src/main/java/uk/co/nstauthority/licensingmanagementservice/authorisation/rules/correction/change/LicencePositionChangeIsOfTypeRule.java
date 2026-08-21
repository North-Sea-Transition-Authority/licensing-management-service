package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.AccessInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;

@Component
@Order(10)
public class LicencePositionChangeIsOfTypeRule implements AccessInterceptorRule {

  private final LicencePositionChangeService licencePositionChangeService;

  public LicencePositionChangeIsOfTypeRule(LicencePositionChangeService licencePositionChangeService) {
    this.licencePositionChangeService = licencePositionChangeService;
  }

  @Override
  public Class<? extends Annotation> supports() {
    return LicencePositionChangeIsOfType.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation, HttpServletRequest request, HttpServletResponse response) {
    var operationTypes = ((LicencePositionChangeIsOfType) annotation).value();

    if (operationTypes.length == 0) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "No operation types provided to security annotation"
      );
    }

    var changeId = getPathVariableEntityIdFromRequest(request, "changeId");

    var change = licencePositionChangeService.findById(changeId).orElse(null);

    if (change == null) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(
          HttpStatus.NOT_FOUND, "No licence position change %s".formatted(changeId)
      );
    }

    var carriesOperation = Arrays.stream(operationTypes)
        .anyMatch(operationType -> LicencePositionChangeOperationUtil.containsOperation(change, operationType));

    if (!carriesOperation) {
      return SecurityRuleResult.checkFailedWithStatusAndMessage(HttpStatus.NOT_FOUND,
          "Change %s does not carry a %s".formatted(changeId, describe(operationTypes))
      );
    }

    return SecurityRuleResult.continueAsNormal();
  }

  private String describe(Class<?>[] operationTypes) {
    return Arrays.stream(operationTypes)
        .map(Class::getSimpleName)
        .collect(Collectors.joining(" or "));
  }
}

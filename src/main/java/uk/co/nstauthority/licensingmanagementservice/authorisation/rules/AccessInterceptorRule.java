package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;

public interface AccessInterceptorRule {

  Class<? extends Annotation> supports();

  SecurityRuleResult check(Object annotation,
                           HttpServletRequest request,
                           HttpServletResponse response);

  default UUID getPathVariableEntityIdFromRequest(
      HttpServletRequest request,
      String pathVariableName
  ) {
    var pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

    var pathVariableIdString = pathVariables.get(pathVariableName);

    if (pathVariableIdString == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Path variable %s not found in request".formatted(pathVariableName)
      );
    }

    UUID pathVariableEntityId;

    try {
      pathVariableEntityId = UUID.fromString(pathVariableIdString);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UUID parse error", e);
    }
    return pathVariableEntityId;
  }
}

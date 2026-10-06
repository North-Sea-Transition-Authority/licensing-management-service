package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.authorisation.SecurityRuleResult;
import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableEntity;

public interface AccessInterceptorRule {

  Class<? extends Annotation> supports();

  SecurityRuleResult check(Object annotation,
                           HttpServletRequest request,
                           HttpServletResponse response);

  default UUID getPathVariableEntityIdFromRequest(
      HttpServletRequest request,
      Class<?> entityClass
  ) {
    return parseUuid(request, getPathVariableName(entityClass));
  }

  @Deprecated
  default UUID getPathVariableEntityIdFromRequest(
      HttpServletRequest request,
      String pathVariableName
  ) {
    return parseUuid(request, pathVariableName);
  }

  default Integer getPathVariableIdInteger(
      HttpServletRequest request,
      Class<?> entityClass
  ) {
    return parseInteger(request, getPathVariableName(entityClass));
  }

  @Deprecated
  default Integer getPathVariableIdInteger(
      HttpServletRequest request,
      String pathVariableName
  ) {
    return parseInteger(request, pathVariableName);
  }

  private static String getPathVariableName(Class<?> entityClass) {
    var pathVariableEntity = AnnotationUtils.findAnnotation(entityClass, PathVariableEntity.class);

    if (pathVariableEntity == null) {
      throw new IllegalStateException(
          "Class %s is not annotated with @PathVariableEntity".formatted(entityClass.getName())
      );
    }
    return pathVariableEntity.pathVariableName();
  }

  private static UUID parseUuid(
      HttpServletRequest request,
      String pathVariableName
  ) {
    var pathVariableIdString = getPathVariableIdString(request, pathVariableName);

    try {
      return UUID.fromString(pathVariableIdString);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UUID parse error", e);
    }
  }

  private static Integer parseInteger(
      HttpServletRequest request,
      String pathVariableName
  ) {
    var pathVariableIdString = getPathVariableIdString(request, pathVariableName);

    try {
      return Integer.valueOf(pathVariableIdString);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Integer parse error", e);
    }
  }

  @NotNull
  private static String getPathVariableIdString(
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
    return pathVariableIdString;
  }
}

package uk.co.nstauthority.licensingmanagementservice.argumentresolver;

import java.util.Map;
import java.util.Optional;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableEntity;

/**
 * This interface is designed to work with entities which are used in ReverseRouter operations. It aims to reduce boilerplate
 * and have stricter typing than the generic Spring interface. Implementations are automatically added to Spring's MVC
 * argument resolvers list
 *
 * @param <T> The type of entity that this resolver will resolve. Must be annotated with @PathVariableEntity so this can
 *           figure out which path variable to use when attempting to resolve the entity
 */
public interface HandlerMethodEntityResolver<T> extends HandlerMethodArgumentResolver {

  Class<T> entityClass();

  Optional<T> resolve(String id);

  @Override
  default boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(entityClass());
  }

  default Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory
  ) {
    var pathVariable = AnnotationUtils.findAnnotation(entityClass(), PathVariableEntity.class).pathVariableName();

    var request = ((ServletWebRequest) webRequest).getRequest();
    var id = Optional.ofNullable(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .map(o -> (Map<String, String>) o)
        .map(map -> map.get(pathVariable))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    try {
      return resolve(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UUID parse error", e);
    }
  }

}
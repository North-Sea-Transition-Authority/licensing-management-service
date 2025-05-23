package uk.co.nstauthority.licensingmanagementservice.mvc;

import jakarta.persistence.Id;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableEntity;

public class ReverseRouter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReverseRouter.class);

  private ReverseRouter() {
    throw new IllegalStateException("ReverseRouter is a static utility class and should not be instantiated");
  }

  public static String route(Object methodCall) {
    return route(methodCall, Collections.emptyMap(), true);
  }

  public static String route(Object methodCall, Map<String, Object> uriVariables) {
    return route(methodCall, uriVariables, true);
  }

  @SuppressWarnings("unchecked")
  public static String route(Object methodCall,
                             Map<String, Object> uriVariables,
                             boolean expandUriVariablesFromRequest) {
    //Establish URI variables to substitute - explicitly provided should take precedence
    Map<String, Object> allUriVariables = new HashMap<>();

    if (expandUriVariablesFromRequest) {
      RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

      if (Objects.nonNull(requestAttributes)) {
        var requestAttributeMap = (Map<String, Object>) requestAttributes.getAttribute(
            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            RequestAttributes.SCOPE_REQUEST
        );

        if (requestAttributeMap != null) {
          allUriVariables.putAll(requestAttributeMap);
        }
      }
    }

    allUriVariables.putAll(uriVariables);
    allUriVariables.putAll(getPathVariableEntityIds(methodCall));

    // Use a UriComponentsBuilder which is not scoped to the request to get relative URIs (instead of absolute)
    var uriComponentsBuilder = UriComponentsBuilder.newInstance();
    return MvcUriComponentsBuilder.fromMethodCall(uriComponentsBuilder, methodCall)
        .buildAndExpand(allUriVariables)
        .toUriString();
  }

  private static Map<String, Object> getPathVariableEntityIds(Object methodCall) {
    var uriVariables = new HashMap<String, Object>();
    var argumentValues = ((MvcUriComponentsBuilder.MethodInvocationInfo) methodCall).getArgumentValues();
    for (Object currentArgument : argumentValues) {
      if (currentArgument == null) {
        continue;
      }

      var clazz = currentArgument.getClass();
      var annotation = AnnotationUtils.findAnnotation(clazz, PathVariableEntity.class);

      if (annotation != null) {
        try {
          var idField = FieldUtils.getFieldsListWithAnnotation(clazz, Id.class);

          if (idField.size() != 1) {
            throw new IllegalStateException("Must have exactly one @Id annotation on class %s, found %d"
                .formatted(clazz.getName(), idField.size()));
          }

          var field = idField.getFirst();
          ReflectionUtils.makeAccessible(field);
          var id = field.get(currentArgument);
          var pathVariableName = clazz.getAnnotation(PathVariableEntity.class).pathVariableName();
          uriVariables.put(pathVariableName, id);

        } catch (IllegalAccessException ignored) {
          LOGGER.warn("Cannot access path variable id from class %s".formatted(clazz.getName()));
        }
      }
    }
    return uriVariables;
  }

  public static ModelAndView redirect(Object methodCall) {
    return redirect(methodCall, Collections.emptyMap());
  }

  public static ModelAndView redirect(Object methodCall, Map<String, Object> uriVariables) {
    return redirect(methodCall, uriVariables, true);
  }

  public static ModelAndView redirect(Object methodCall,
                                      Map<String, Object> uriVariables,
                                      boolean expandUriVariablesFromRequest) {
    return new ModelAndView("redirect:" + route(methodCall, uriVariables, expandUriVariablesFromRequest));
  }

  /**
   * Return an empty BindingResult.
   * Used to avoid passing null into a BindingResult route parameter, which then will cause null warnings to be thrown by IJ if
   * the controller invokes methods on the BindingResult, as it now thinks null is a possible runtime value.
   *
   * @return An empty BindingResult
   */
  public static BindingResult emptyBindingResult() {
    return new BeanPropertyBindingResult(null, "empty");
  }
}

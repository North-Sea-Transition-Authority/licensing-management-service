package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class LicenceTypeArgumentResolver implements HandlerMethodArgumentResolver {

  public static final String LICENCE_TYPE_SLUG = "licenceTypeSlug";

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(LicenceType.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object resolveArgument(
      @NonNull MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      @NonNull NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory
  ) {
    HttpServletRequest request = ((ServletWebRequest) webRequest).getRequest();
    Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(
        HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
    );
    String slug = pathVariables != null ? pathVariables.get(LICENCE_TYPE_SLUG) : null;

    return Arrays.stream(LicenceType.values())
        .filter(type -> type.getUrlSlug().equals(slug))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No licence type for url slug %s".formatted(slug)
        ));
  }
}

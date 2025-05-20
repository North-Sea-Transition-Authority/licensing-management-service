package uk.co.nstauthority.template.authorisation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import uk.co.nstauthority.template.authorisation.rules.AccessInterceptorRule;

@Component
public class AccessHandlerInterceptor implements HandlerInterceptor {

  private final List<AccessInterceptorRule> securityRules;

  public AccessHandlerInterceptor(List<AccessInterceptorRule> securityRules) {
    this.securityRules = securityRules;
  }

  @Override
  public boolean preHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler) {
    if (handler instanceof ResourceHttpRequestHandler) {
      return true;
    }

    var handlerMethod = (HandlerMethod) handler;

    for (var securityRule : securityRules) {
      var annotationObject = findMethodOrClassAnnotation(securityRule.supports(), handlerMethod);
      if (annotationObject.isEmpty()) {
        continue;
      }

      var result = securityRule.check(
          annotationObject.get(),
          request,
          response
      );

      if (!result.hasRulePassed()) {
        if (result.failureStatus() == null) {
          throw new RuntimeException("securityRuleResult has failed without failure status");
        } else {
          throw new ResponseStatusException(result.failureStatus(), result.failureMessage());
        }
      }
    }
    return true;
  }

  private static <A extends Annotation> Optional<A> findMethodOrClassAnnotation(Class<A> annotationClass,
                                                                               HandlerMethod handlerMethod) {
    var method = handlerMethod.getMethod();
    // Check if the method has the desired annotation
    var methodAnnotation = AnnotationUtils.findAnnotation(method, annotationClass);
    if (methodAnnotation != null) {
      return Optional.of(methodAnnotation);
    }

    // Fallback and check if the class contains the annotation
    return Optional.ofNullable(AnnotationUtils.findAnnotation(method.getDeclaringClass(), annotationClass));
  }

}

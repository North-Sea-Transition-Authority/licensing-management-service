package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.AnnotationUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractInterceptorRuleTest {

  @Mock
  protected HttpServletRequest request;

  @Mock
  protected HttpServletResponse response;

  protected <A extends Annotation> A getAnnotation(Class<?> controllerClass, Class<A> annotationClass) {
    var annotation = AnnotationUtils.findAnnotation(controllerClass, annotationClass);
    assertThat(annotation).isPresent();
    return annotation.get();
  }

  protected <A extends Annotation> A getAnnotation(Method method, Class<A> annotationClass) {
    var annotation = AnnotationUtils.findAnnotation(method, annotationClass);
    assertThat(annotation).isPresent();
    return annotation.get();
  }
}

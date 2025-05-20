package uk.co.nstauthority.template.authorisation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import uk.co.nstauthority.template.authorisation.rules.AccessInterceptorRule;

public class ExampleTrueOrFailWithoutStatusAccessSecurityRule implements AccessInterceptorRule {

  @Override
  public Class<? extends Annotation> supports() {
    return ExampleAnnotation.class;
  }

  @Override
  public SecurityRuleResult check(Object annotation, HttpServletRequest request, HttpServletResponse response) {
    var exampleAnnotation = (ExampleAnnotation) annotation;

    if (exampleAnnotation.value()) {
      return SecurityRuleResult.continueAsNormal();
    } else {
      return SecurityRuleResult.checkFailedWithStatus(null);
    }
  }
}

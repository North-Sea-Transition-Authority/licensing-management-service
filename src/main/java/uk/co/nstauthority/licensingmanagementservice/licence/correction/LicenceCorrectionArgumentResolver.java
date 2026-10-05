package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LicenceCorrectionArgumentResolver implements HandlerMethodArgumentResolver {

  public static final String VALIDATED_CORRECTION = "validatedCorrection";

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(LicenceCorrection.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
    var correction = ((ServletWebRequest) webRequest).getRequest().getAttribute(VALIDATED_CORRECTION);
    if (correction == null) {
      throw new IllegalStateException(
          "No %s on request. Is @InvokingUserCanViewCorrection missing?".formatted(VALIDATED_CORRECTION)
      );
    }
    return correction;
  }

}
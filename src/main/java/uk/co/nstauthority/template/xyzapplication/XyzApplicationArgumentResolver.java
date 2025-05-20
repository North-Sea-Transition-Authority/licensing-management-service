package uk.co.nstauthority.template.xyzapplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.template.util.RequestUtil;

@Component
public class XyzApplicationArgumentResolver implements HandlerMethodArgumentResolver {

  static final String XYZ_APPLICATION_ID = "applicationId";

  private final XyzApplicationService xyzApplicationService;

  @Autowired
  public XyzApplicationArgumentResolver(XyzApplicationService xyzApplicationService) {
    this.xyzApplicationService = xyzApplicationService;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(XyzApplication.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
    var applicationId = RequestUtil.getId(((ServletWebRequest) webRequest).getRequest(), XYZ_APPLICATION_ID)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Missing required %s param".formatted(XYZ_APPLICATION_ID)
        ));

    return xyzApplicationService.findXyzApplicationById(applicationId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No Xyz application found with id %s".formatted(applicationId)
        ));
  }


}

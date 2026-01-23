package uk.co.nstauthority.licensingmanagementservice.licence;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.util.RequestUtil;

@Component
public class LicenceArgumentResolver implements HandlerMethodArgumentResolver {

  public static final String LICENCE_ID = "licenceId";

  private final LicenceService licenceService;

  public LicenceArgumentResolver(LicenceService licenceService) {
    this.licenceService = licenceService;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(Licence.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
    var licenceId = RequestUtil.getIntegerId(((ServletWebRequest) webRequest).getRequest(), LICENCE_ID)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Missing required %s param".formatted(LICENCE_ID)
        ));

    return licenceService.findLicenceByIdOrThrow(licenceId);
  }

}

package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

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
public class LicenceContinuationApplicationDetailArgumentResolver implements HandlerMethodArgumentResolver {

  private final LicenceContinuationService licenceContinuationService;

  public LicenceContinuationApplicationDetailArgumentResolver(LicenceContinuationService licenceContinuationService) {
    this.licenceContinuationService = licenceContinuationService;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(LicenceContinuationApplicationDetail.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
    var licenceContinuationApplicationDetailId = RequestUtil.getId(((ServletWebRequest) webRequest).getRequest(),
            LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Missing required %s param".formatted(
                LicenceContinuationApplicationDetail.LICENCE_CONTINUATION_APPLICATION_DETAIL_ID
            )
        ));

    return licenceContinuationService.getDetailByIdOrThrow(licenceContinuationApplicationDetailId);
  }

}

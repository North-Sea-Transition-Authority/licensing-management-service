package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

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
public class LicenceScheduleDetailArgumentResolver implements HandlerMethodArgumentResolver {

  static final String LICENCE_SCHEDULE_DETAIL_ID = "licenceScheduleDetailId";

  private final LicenceScheduleDetailService licenceScheduleDetailService;

  public LicenceScheduleDetailArgumentResolver(LicenceScheduleDetailService licenceScheduleDetailService) {
    this.licenceScheduleDetailService = licenceScheduleDetailService;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(LicenceScheduleDetail.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
    var scheduleDetailId = RequestUtil.getId(((ServletWebRequest) webRequest).getRequest(), LICENCE_SCHEDULE_DETAIL_ID)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Missing required %s param".formatted(LICENCE_SCHEDULE_DETAIL_ID)
        ));

    return licenceScheduleDetailService.getByIdOrThrow(scheduleDetailId);
  }
}

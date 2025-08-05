package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

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
public class ScheduleWorkProgrammeApplicationDetailArgumentResolver implements HandlerMethodArgumentResolver {

  static final String SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID = "scheduleWorkProgrammeApplicationDetailId";

  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  public ScheduleWorkProgrammeApplicationDetailArgumentResolver(
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService) {
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(ScheduleWorkProgrammeApplicationDetail.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
    var scheduleWorkProgrammeApplicationDetailId = RequestUtil.getId(((ServletWebRequest) webRequest).getRequest(),
            SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Missing required %s param".formatted(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL_ID)
        ));

    return scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(scheduleWorkProgrammeApplicationDetailId);
  }

}

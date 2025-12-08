package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

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
public class WorkProgrammeActivityArgumentResolver implements HandlerMethodArgumentResolver {

  static final String WORK_PROGRAMME_ACTIVITY_ID = "workProgrammeActivityId";

  private final WorkProgrammeActivityService workProgrammeActivityService;

  public WorkProgrammeActivityArgumentResolver(
      WorkProgrammeActivityService workProgrammeActivityService
  ) {
    this.workProgrammeActivityService = workProgrammeActivityService;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(WorkProgrammeActivity.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
    var workProgrammeActivityId = RequestUtil.getId(((ServletWebRequest) webRequest).getRequest(), WORK_PROGRAMME_ACTIVITY_ID)
                                             .orElseThrow(() -> new ResponseStatusException(
                                                 HttpStatus.NOT_FOUND,
                                                 "Missing required %s param".formatted(WORK_PROGRAMME_ACTIVITY_ID)
                                             ));

    return workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(workProgrammeActivityId);
  }
}
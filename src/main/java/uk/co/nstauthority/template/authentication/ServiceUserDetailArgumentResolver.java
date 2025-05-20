package uk.co.nstauthority.template.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class ServiceUserDetailArgumentResolver implements HandlerMethodArgumentResolver {

  private final UserDetailService userDetailService;

  @Autowired
  public ServiceUserDetailArgumentResolver(UserDetailService userDetailService) {
    this.userDetailService = userDetailService;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(ServiceUserDetail.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
    return userDetailService.getUserDetail();
  }
}

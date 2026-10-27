package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

// Runs after the handler, so the navigation reflects any change the page's own request made to the journey
@Component
@Profile("mockups")
class UploadDspMockupNavigationInterceptor implements HandlerInterceptor {

  private final UploadDspMockupNavigation navigation;

  UploadDspMockupNavigationInterceptor(UploadDspMockupNavigation navigation) {
    this.navigation = navigation;
  }

  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView
  ) {
    var viewName = modelAndView == null ? null : modelAndView.getViewName();
    if (viewName == null || viewName.startsWith("redirect:")) {
      return;
    }
    var path = request.getRequestURI().substring(request.getContextPath().length());
    modelAndView.addObject("mockupActors", navigation.forPath(path));
  }
}

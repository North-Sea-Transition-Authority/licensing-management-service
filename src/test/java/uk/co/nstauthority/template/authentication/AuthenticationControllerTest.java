package uk.co.nstauthority.template.authentication;

import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.template.AbstractControllerTest;
import uk.co.nstauthority.template.mvc.ReverseRouter;
import uk.co.nstauthority.template.util.SecurityTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.template.authentication.TestUserProvider.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.co.nstauthority.template.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

@ContextConfiguration(classes = {
    AuthenticationControllerTest.TestAuthenticationController.class
})
class AuthenticationControllerTest extends AbstractControllerTest {

  @Controller
  @RequestMapping("/auth")
  static class TestAuthenticationController {

    @GetMapping("/secured")
    public ModelAndView renderSecured() {
      return new ModelAndView("stub");
    }

  }

  @SecurityTest
  void authenticationRequired() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(TestAuthenticationController.class)
            .renderSecured())))
        .andExpect(redirectionToLoginUrl());
  }

  @SecurityTest
  void authorisedRequest() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(TestAuthenticationController.class)
            .renderSecured()))
            .with(user(ServiceUserDetailTestUtil.newBuilder().build())))
        .andExpect(status().isOk());
  }
}

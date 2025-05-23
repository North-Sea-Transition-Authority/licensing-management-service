package uk.co.nstauthority.licensingmanagementservice.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.branding.CustomerConfigurationProperties;
import uk.co.nstauthority.licensingmanagementservice.branding.ServiceConfigurationProperties;

@ContextConfiguration(
    classes = {
        DefaultPageControllerAdviceTest.TestController.class,
        DefaultPageControllerAdvice.class
    })
class DefaultPageControllerAdviceTest extends AbstractControllerTest {

  @Test
  void addDefaultModelAttributes_verifyDefaultAttributes() throws Exception {

    var modelAndView =
        mockMvc
            .perform(
                get(ReverseRouter.route(on(TestController.class).testEndpoint()))
                    .with(user(ServiceUserDetailTestUtil.newBuilder().withWuaId(1L).build())))
            .andReturn()
            .getModelAndView();

    assertThat(modelAndView).isNotNull();

    var modelMap = modelAndView.getModel();

    assertThat((CustomerConfigurationProperties) modelMap.get("customerBranding"))
        .hasNoNullFieldsOrProperties();
    assertThat((ServiceConfigurationProperties) modelMap.get("serviceBranding"))
        .hasNoNullFieldsOrProperties();
    assertThat(modelMap.get("accessibilityStatementUrl")).isNotNull();
    assertThat(modelMap.get("privacyUrl")).isNotNull();
    assertThat(modelMap.get("cookiePolicyUrl")).isNotNull();
    assertThat(modelMap.get("contactPageUrl")).isNotNull();
    assertThat(modelMap.get("serviceHomeUrl")).isNotNull();
    AssertionsForClassTypes.assertThat((ServiceUserDetail) modelMap.get("loggedInUser"))
        .extracting(ServiceUserDetail::wuaId)
        .isEqualTo(1L);
  }

  // Dummy application to stop the @WebMvcTest loading more than it needs
  @SpringBootApplication
  static class TestApplication {
  }

  @Controller
  @RequestMapping("/endpoint")
  static class TestController {

    @GetMapping()
    ModelAndView testEndpoint() {
      return new ModelAndView("stub");
    }
  }
}

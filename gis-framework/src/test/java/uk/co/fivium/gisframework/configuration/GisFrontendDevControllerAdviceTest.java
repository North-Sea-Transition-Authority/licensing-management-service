package uk.co.fivium.gisframework.configuration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisframework.AbstractControllerTest;

@ContextConfiguration(
    classes = {
        GisFrontendDevControllerAdviceTest.TestController.class,
        GisFrontendDevControllerAdvice.class
    })
@TestPropertySource(properties = "gis-framework.vite-dev-server-url=vite-dev-url")
@ActiveProfiles("localdev-vue-hmr")
class GisFrontendDevControllerAdviceTest extends AbstractControllerTest {

  @Test
  void gisViteDevServerUrl() throws Exception {
    mockMvc
        .perform(get("/test"))
        .andExpect(model().attribute("gisViteDevServerUrl", "vite-dev-url"));
  }

  @Controller
  @RequestMapping("/test")
  static class TestController {

    @GetMapping
    ModelAndView testEndpoint() {
      return new ModelAndView("stub");
    }
  }
}

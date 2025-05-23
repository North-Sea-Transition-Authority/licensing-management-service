package uk.co.nstauthority.licensingmanagementservice.mvc.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = ErrorSummaryItemsHandlerInterceptorTest.ErrorSummaryItemsHandlerTestController.class)
class ErrorSummaryItemsHandlerInterceptorTest extends AbstractControllerTest {

  @Test
  void getErrorItems_whenHasBindingResult_thenIncludeErrorList() throws Exception {

    var errorList = List.of(new ErrorSummaryItem(0, "ErrorMessage", "default message"));

    var modelAndView = mockMvc.perform(
            post(ReverseRouter.route(on(ErrorSummaryItemsHandlerTestController.class)
                .endpointWithBindingResult(new Form(), null)))
                .with(user(regulatorUser))
                .with(csrf()))
        .andExpect(view().name(ErrorSummaryItemsHandlerTestController.VIEW_NAME))
        .andReturn().getModelAndView();

    assertThat(modelAndView.getModel().get("errorSummaryItems"))
        .usingRecursiveComparison()
        .isEqualTo(errorList);
  }

  @Test
  void getErrorItems_whenNoBindingResult_thenNoErrorList() throws Exception {
    mockMvc.perform(
            post(ReverseRouter.route(on(ErrorSummaryItemsHandlerTestController.class)
                .endpointWithoutBindingResult()))
                .with(user(regulatorUser))
                .with(csrf()))
        .andExpect(view().name(ErrorSummaryItemsHandlerTestController.VIEW_NAME))
        .andExpect(model().attributeDoesNotExist("errorSummaryItems"));
  }

  @Controller
  @RequestMapping
  static class ErrorSummaryItemsHandlerTestController {

    static final String VIEW_NAME = "lms/error/notFound.ftl";

    @PostMapping("no-binding-result")
    public ModelAndView endpointWithoutBindingResult() {
      return new ModelAndView(VIEW_NAME);
    }

    @PostMapping("with-binding-result")
    public ModelAndView endpointWithBindingResult(@ModelAttribute("form") Form form, BindingResult bindingResult) {
      bindingResult.addError(new FieldError("form","ErrorMessage","default message"));
      return new ModelAndView(VIEW_NAME);
    }
  }

  static class Form {
    private String field;

    public String getField() {
      return field;
    }

    public void setField(String field) {
      this.field = field;
    }
  }
}

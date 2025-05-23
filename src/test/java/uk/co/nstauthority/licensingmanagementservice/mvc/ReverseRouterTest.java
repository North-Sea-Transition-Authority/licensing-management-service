package uk.co.nstauthority.licensingmanagementservice.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableEntity;

@ExtendWith(SpringExtension.class)
@WebMvcTest
class ReverseRouterTest extends AbstractControllerTest {

  @BeforeEach
  void setUp() {

    Map<String, Object> uriTemplateVariablesMap = Map.of(
        "parentId", "request_parent_id",
        "childId", "request_child_id"
    );

    var mockHttpServletRequest = new MockHttpServletRequest();
    var servletRequestAttributes = new ServletRequestAttributes(mockHttpServletRequest);

    servletRequestAttributes.setAttribute(
        HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
        uriTemplateVariablesMap,
        RequestAttributes.SCOPE_REQUEST
    );

    RequestContextHolder.setRequestAttributes(servletRequestAttributes);
  }

  @Test
  void route_methodCallVariant() {
    var grandchild = new Grandchild();
    grandchild.setId("method_grandchild_id");
    var route = ReverseRouter.route(
        on(TestController.class).testMethod("method_child_id", grandchild));
    assertThat(route).isEqualTo("/parent/request_parent_id/child/method_child_id/method_grandchild_id");
  }

  @Test
  void route_whenUriVariablesVariant_thenVerifyRoute() {
    var grandchild = new Grandchild();
    grandchild.setId("method_grandchild_id");
    // variable from request should be overridden by variable in map
    var route = ReverseRouter.route(
        on(TestController.class).testMethod("method_child_id", grandchild),
        Map.of("parentId", "map_parent_id")
    );
    assertThat(route).isEqualTo("/parent/map_parent_id/child/method_child_id/method_grandchild_id");
  }

  @Test
  void route_whenPathVariableEntityHasMultipleIds_thenException() {
    var doubleIdGrandchild = new DoubleIdGrandchild();
    doubleIdGrandchild.setId("method_doubleIdgrandchild_id");
    doubleIdGrandchild.setSecondId("method_doubleIdgrandchild_secondId");
    // should throw an exception that the DoubleIdGrandchild entity should only have one @ID annotation
    assertThrowsExactly(
        IllegalStateException.class,
        () -> ReverseRouter.route(
            on(TestController.class).testMethod("method_child_id", doubleIdGrandchild),
            Map.of("parentId", "map_parent_id")
        ),
        "Must have exactly one @Id annotation on class DoubleIdGrandchild.class, found 2"
    );
  }

  @Test
  void route_whenExpandUriVariablesFromRequestFalse_thenVariableFromRequestOverriddenMyMap() {
    var grandchild = new Grandchild();
    grandchild.setId("method_grandchild_id");
    // variable from request should be overridden by variable in map even when request substitution is disabled
    var route = ReverseRouter.route(
        on(TestController.class).testMethod("method_child_id", grandchild),
        Map.of("parentId", "map_parent_id"),
        false
    );
    assertThat(route).isEqualTo("/parent/map_parent_id/child/method_child_id/method_grandchild_id");
  }

  @Test
  void route_whenMethodParamNotOverriddenByVariableInMap_thenVerifyRoute() {
    var requestGrandchild = new Grandchild();
    requestGrandchild.setId("method_grandchild_id");
    var mapGrandchild = new Grandchild();
    mapGrandchild.setId("map_grandchild_id");
    // variable from method parameter should NOT be overridden by variable in map
    var route = ReverseRouter.route(
        on(TestController.class).testMethod("method_child_id", requestGrandchild),
        Map.of(
            "parentId", "map_parent_id",
            "childId", "map_child_id",
            "grandchildId", mapGrandchild
        )
    );
    assertThat(route).isEqualTo("/parent/map_parent_id/child/method_child_id/method_grandchild_id");
  }

  @Test
  void route_whenNoVariablesAllowedFromRequest_thenException() {
    var grandchild = new Grandchild();
    grandchild.setId("method_grandchild_id");
    // should throw exception if we don't allow variables from the request
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> ReverseRouter.route(
            on(TestController.class).testMethod("method_child_id", grandchild),
            Map.of(),
            false),
        "Map has no value"
    );
  }

  @Test
  void redirect_whenMethodCallVariant_thenVerifyRoute() {
    var grandchild = new Grandchild();
    grandchild.setId("method_grandchild_id");
    // redirect should behave as route does, with "redirect:/" prefix on results
    var redirectModelAndView =
        ReverseRouter.redirect(
            on(TestController.class).testMethod("method_child_id", grandchild));
    assertThat(redirectModelAndView.getViewName())
        .isEqualTo("redirect:/parent/request_parent_id/child/method_child_id/method_grandchild_id");
  }

  @Test
  void redirect_whenPathVariableEntityHasMultipleIds_thenException() {
    var doubleIdGrandchild = new DoubleIdGrandchild();
    doubleIdGrandchild.setId("method_doubleIdgrandchild_id");
    doubleIdGrandchild.setSecondId("method_doubleIdgrandchild_secondId");
    // redirect should behave the same as route,
    // and should throw an exception that the DoubleIdGrandchild entity should only have one @ID annotation
    assertThrowsExactly(
        IllegalStateException.class,
        () -> ReverseRouter.redirect(
            on(TestController.class).testMethod("method_child_id", doubleIdGrandchild),
            Map.of("parentId", "map_parent_id")
        ),
        "Must have exactly one @Id annotation on class DoubleIdGrandchild.class, found 2"
    );
  }

  @Test
  void redirect_whenUriVariablesVariant_thenVerifyRoute() {
    var grandchild = new Grandchild();
    grandchild.setId("method_grandchild_id");
    // redirect should behave as route does, with "redirect:/" prefix on results
    var redirectModelAndView = ReverseRouter.redirect(
        on(TestController.class).testMethod("method_child_id", grandchild),
        Map.of("parentId", "map_parent_id")
    );
    assertThat(redirectModelAndView.getViewName())
        .isEqualTo("redirect:/parent/map_parent_id/child/method_child_id/method_grandchild_id");
  }

  @Test
  void redirect_whenExpandUriVariablesFromRequestFalse_thenVerifyRoute() {
    var grandchild = new Grandchild();
    grandchild.setId("method_grandchild_id");
    // redirect should behave as route does, with "redirect:/" prefix on results
    var redirectModelAndView = ReverseRouter.redirect(
        on(TestController.class).testMethod("method_child_id", grandchild),
        Map.of("parentId", "map_parent_id"),
        false
    );
    assertThat(redirectModelAndView.getViewName())
        .isEqualTo("redirect:/parent/map_parent_id/child/method_child_id/method_grandchild_id");
  }

  @Test
  void redirect_whenMethodParamNotOverriddenByVariableInMap_thenVerifyRoute() {
    var requestGrandchild = new Grandchild();
    requestGrandchild.setId("method_grandchild_id");
    var mapGrandchild = new Grandchild();
    mapGrandchild.setId("map_grandchild_id");
    // variable from method parameter should NOT be overridden by variable in map
    var redirectModelAndView = ReverseRouter.redirect(
        on(TestController.class).testMethod("method_child_id", requestGrandchild),
        Map.of(
            "parentId", "map_parent_id",
            "childId", "map_child_id",
            "grandchildId", mapGrandchild
        )
    );
    assertThat(redirectModelAndView.getViewName())
        .isEqualTo("redirect:/parent/map_parent_id/child/method_child_id/method_grandchild_id");
  }

  @Test
  void emptyBindingResult_verifyResponse() {
    var emptyBindingResult = (BeanPropertyBindingResult) ReverseRouter.emptyBindingResult();
    assertThat(emptyBindingResult.getTarget()).isNull();
    assertThat(emptyBindingResult.getObjectName()).isEqualTo("empty");
  }

  // Dummy application to stop the @WebMvcTest loading more than it needs
  @SpringBootApplication
  static class TestApplication {
  }

  @RequestMapping("/parent/{parentId}")
  static class TestController {

    @GetMapping("/child/{childId}/{grandchildId}")
    ModelAndView testMethod(
        @PathVariable String childId,
        Grandchild grandchild
    ) {
      return new ModelAndView("%s/%s".formatted(childId, grandchild.getId()));
    }

    @GetMapping("/child/{childId}/{grandchildId}")
    ModelAndView testMethod(
        @PathVariable String childId,
        DoubleIdGrandchild doubleIdGrandchild
    ) {
      return new ModelAndView("%s/%s/%s".formatted(
          childId,
          doubleIdGrandchild.getId(),
          doubleIdGrandchild.getSecondId()
      ));
    }
  }

  @Entity
  @PathVariableEntity(pathVariableName = "grandchildId")
  static class Grandchild {

    @Id
    private String id;

    String getId() {
      return id;
    }

    void setId(String id) {
      this.id = id;
    }
  }

  @Entity
  @PathVariableEntity(pathVariableName = "grandchildId")
  static class DoubleIdGrandchild {

    @Id
    private String id;

    @Id
    private String secondId;

    String getId() {
      return id;
    }

    void setId(String id) {
      this.id = id;
    }

    String getSecondId() {
      return secondId;
    }

    void setSecondId(String secondId) {
      this.secondId = secondId;
    }
  }
}

package uk.co.nstauthority.licensingmanagementservice.authorisation;


import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

public class TestControllers {

  @Controller
  @RequestMapping("{applicationid}")
  static class FalseController {
    @GetMapping("/test-false")
    @ExampleAnnotation(false)
    String get(@PathVariable UUID applicationid) {
      return "some data";
    }
  }

  @Controller
  @RequestMapping("{applicationid}")
  @ExampleAnnotation(true)
  static class TrueController {
    @GetMapping("/test-true")
    String get(@PathVariable UUID applicationid) {
      return "some data";
    }
  }
}

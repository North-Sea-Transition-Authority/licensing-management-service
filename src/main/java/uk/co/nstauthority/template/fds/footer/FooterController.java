package uk.co.nstauthority.template.fds.footer;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class FooterController {
  @GetMapping("/accessibility-statement")
  public ModelAndView accessibilityStatement() {
    return new ModelAndView("xyz/layout/footer/accessibilityStatement");
  }

  @GetMapping("/cookies")
  public ModelAndView cookies() {
    return new ModelAndView("xyz/layout/footer/cookies");
  }

  @GetMapping("/contact")
  public ModelAndView contact() {
    return new ModelAndView("xyz/layout/footer/contact");
  }
}

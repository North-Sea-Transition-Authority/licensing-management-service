package uk.co.nstauthority.licensingmanagementservice.mvc.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.co.nstauthority.licensingmanagementservice.topnavigation.TopNavigationService;

@ExtendWith(MockitoExtension.class)
class DefaultExceptionResolverTest {

  @Mock
  private ErrorService errorService;

  @Mock
  private TopNavigationService topNavigationService;

  @InjectMocks
  private DefaultExceptionResolver defaultExceptionResolver;

  @Test
  void getModelAndView_assertTopNavigationItemsExist() {
    var viewName = "viewName";
    var request = new MockHttpServletRequest();

    var modelAndView = defaultExceptionResolver.getModelAndView(viewName, new IllegalStateException(), request);

    assertThat(modelAndView.getModel()).containsKeys("navigationItems", "currentEndPoint");
  }
}

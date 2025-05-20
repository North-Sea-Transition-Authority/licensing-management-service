package uk.co.nstauthority.template.mvc.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;


import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.template.configuration.ErrorConfigurationProperties;
import uk.co.nstauthority.template.mvc.ControllerAdviceService;

@ExtendWith(MockitoExtension.class)
class ErrorServiceTest {

  @Mock
  private ControllerAdviceService controllerAdviceService;

  @Mock
  private HttpServletRequest request;

  private ErrorService errorService;

  @BeforeEach
  public void setup() {
    errorService = new ErrorService(
        new ErrorConfigurationProperties(true),
        controllerAdviceService
    );
  }

  @Test
  void addErrorAttributesToModel_whenThrowableError_assertExpectedModelAttributes() {
    var modelAndView = new ModelAndView();

    var resultingModelMap = errorService.addErrorAttributesToModel(
        modelAndView,
        new NullPointerException(),
        request
    ).getModelMap();

    var errorRef = resultingModelMap.get("errorRef");
    assertThat(errorRef).isNotNull();
    assertThat((String) errorRef).containsPattern("^[" + ErrorService.SAFE_CHARACTERS + "]*$");
    assertThat(resultingModelMap.get("stackTrace")).isNotNull();

    verify(controllerAdviceService).addBrandingModelAttributes(modelAndView);
    verify(controllerAdviceService).addCommonUrlModelAttributes(modelAndView);
    verify(controllerAdviceService).addFooterLinkModelAttributes(modelAndView);
    verifyNoMoreInteractions(controllerAdviceService);
  }

  @Test
  void addErrorAttributesToModel_whenNoThrowableError_assertExpectedModelAttributes() {
    var modelAndView = new ModelAndView();

    var resultingModelMap = errorService.addErrorAttributesToModel(modelAndView, null, request).getModelMap();
    assertThat(resultingModelMap).doesNotContainKeys(
        "errorRef",
        "stackTrace"
    );

    verify(controllerAdviceService).addBrandingModelAttributes(modelAndView);
    verify(controllerAdviceService).addCommonUrlModelAttributes(modelAndView);
    verify(controllerAdviceService).addFooterLinkModelAttributes(modelAndView);
    verifyNoMoreInteractions(controllerAdviceService);
  }

}

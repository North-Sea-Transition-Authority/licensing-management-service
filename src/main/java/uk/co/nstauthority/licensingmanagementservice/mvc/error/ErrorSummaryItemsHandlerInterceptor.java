package uk.co.nstauthority.licensingmanagementservice.mvc.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;

/**
 * This is used to pull the binding result object out of the model so that we can attach the errorSummaryItems to the model and
 * view object as part of a post request.
 */
@Component
public class ErrorSummaryItemsHandlerInterceptor implements HandlerInterceptor {

  @Override
  public void postHandle(@NonNull HttpServletRequest request,
                         @NonNull HttpServletResponse response,
                         @NonNull Object handler,
                         ModelAndView modelAndView) {

    String key = BindingResult.MODEL_KEY_PREFIX + "form";
    if (modelAndView != null) {
      BindingResult bindingResult = (BindingResult) modelAndView.getModel().get(key);
      if (bindingResult != null) {
        modelAndView.addObject("errorSummaryItems", getErrorSummaryItems(bindingResult));
      }
    }
  }

  private List<ErrorSummaryItem> getErrorSummaryItems(BindingResult bindingResult) {
    List<ErrorSummaryItem> errorSummaryItems = new ArrayList<>();
    var index = 0;
    for (var errorItem: bindingResult.getFieldErrors()) {
      var fdsErrorItem = new ErrorSummaryItem(index, errorItem.getField(), errorItem.getDefaultMessage());
      errorSummaryItems.add(fdsErrorItem);
      index++;
    }

    return errorSummaryItems;
  }
}

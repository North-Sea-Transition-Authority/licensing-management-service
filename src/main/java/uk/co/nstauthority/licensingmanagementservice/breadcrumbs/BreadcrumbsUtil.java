package uk.co.nstauthority.licensingmanagementservice.breadcrumbs;

import org.springframework.web.servlet.ModelAndView;

public class BreadcrumbsUtil {

  static final String MAP_MODEL_ATRR_NAME = "breadcrumbs";
  static final String CURRENT_PAGE_MODEL_ATRR_NAME = "currentPage";

  private BreadcrumbsUtil() {
    throw new IllegalStateException("BreadcrumbsUtil is an util class and should not be initialized");
  }

  public static void addBreadcrumbsToModel(ModelAndView modelAndView, Breadcrumbs breadcrumbs) {
    modelAndView.addObject(MAP_MODEL_ATRR_NAME, breadcrumbs.toMap());
    modelAndView.addObject(CURRENT_PAGE_MODEL_ATRR_NAME, breadcrumbs.currentPageName());
  }
}

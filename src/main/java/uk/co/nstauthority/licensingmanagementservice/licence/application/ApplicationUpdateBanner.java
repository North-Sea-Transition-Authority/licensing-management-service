package uk.co.nstauthority.licensingmanagementservice.licence.application;

import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;

public final class ApplicationUpdateBanner {

  private ApplicationUpdateBanner() {
  }

  /**
   * Adds the "update requested" banner and the isUpdateDraft flag to a task list page when the detail is an update.
   */
  public static void applyTo(ModelAndView modelAndView, LicenceApplicationDetail applicationDetail) {
    var isUpdate = applicationDetail.isApplicationUpdate();
    modelAndView.addObject("isUpdateDraft", isUpdate);

    if (isUpdate && applicationDetail.getUpdateDeadlineDate() != null) {
      NotificationBanner.newInfoBanner()
          .withHeadingContent("An update has been requested which is due by %s".formatted(
              DateFormatUtil.convertToDisplayText(applicationDetail.getUpdateDeadlineDate())))
          .withOtherContent(applicationDetail.getUpdateReason())
          .applyTo(modelAndView);
    }
  }
}

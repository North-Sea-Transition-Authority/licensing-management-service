package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.withdraw.ApplicationWithdrawReasonValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action.LicenceContinuationActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action.LicenceContinuationActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit.ContinuationSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = LicenceContinuationApplicationWithdrawConfirmationController.class)
class LicenceContinuationApplicationWithdrawConfirmationControllerTest extends AbstractControllerTest {

  @MockitoBean
  LicenceContinuationApplicationOverviewService licenceContinuationApplicationOverviewService;

  @MockitoBean
  ContinuationSummarySectionService continuationSummarySectionService;

  @MockitoBean
  LicenceContinuationActionService licenceContinuationActionService;

  @MockitoBean
  ApplicationWithdrawReasonValidator applicationWithdrawReasonValidator;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE));
  private static final LicenceContinuationApplicationDetail LICENCE_CONTINUATION_APPLICATION_DETAIL = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(LICENCE_SCHEDULE_DETAIL);
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().withWuaId(ORGANISATION_USER_WUA_ID).build();

  @BeforeEach
  void setUp() {
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);

    LICENCE_CONTINUATION_APPLICATION_DETAIL.setStatus(LicenceContinuationApplicationStatus.SUBMITTED);

    when(licenceContinuationActionService.getAvailableUserActionItems(any(), any()))
        .thenReturn(List.of(
            LicenceContinuationActionItem.CONFIRM_CONTINUATION.toActionItemView(LICENCE_CONTINUATION_APPLICATION_DETAIL),
            LicenceContinuationActionItem.WITHDRAW_CONTINUATION.toActionItemView(LICENCE_CONTINUATION_APPLICATION_DETAIL)
        ));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);
  }

  @Test
  void submitForm_whenValid_redirectsToWorkArea() throws Exception {
    String expectedReason = "Business decision changed";
    when(applicationWithdrawReasonValidator.isValid(any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationApplicationWithdrawConfirmationController.class).submitForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null, null, null)))
                .with(user(USER))
                .with(csrf())
                .param("reasonForWithdrawal", expectedReason)
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))));

    verify(licenceContinuationService).withdrawContinuationChangeStatus(LICENCE_CONTINUATION_APPLICATION_DETAIL, expectedReason);
  }

  @Test
  void submitForm_whenInvalid_returnsToFormView() throws Exception {
    when(applicationWithdrawReasonValidator.isValid(any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationApplicationWithdrawConfirmationController.class).submitForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null, null, null)))
                .with(user(USER))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenceContinuationWithdrawConfirmation"))
        .andExpect(model().attributeExists("cancelUrl", "form"));

    verify(licenceContinuationService, never()).withdrawContinuationChangeStatus(any(), any());
  }

  @Test
  void renderWithdrawConfirmation() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationApplicationWithdrawConfirmationController.class).renderWithdrawConfirmation(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null)))
                .with(user(USER))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenceContinuationWithdrawConfirmation"))
        .andExpect(model().attributeExists("cancelUrl", "form"));
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentLinkingService;
import uk.co.nstauthority.licensingmanagementservice.document.instance.LmsDocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.AuthorisationSecurityTest;

@WebMvcTest(ApplicationLetterController.class)
@ContextConfiguration(classes = ApplicationLetterController.class)
class ApplicationLetterControllerTest extends AbstractControllerTest {

  @MockitoBean
  private DocumentInstanceService documentInstanceService;

  @MockitoBean
  private LmsDocumentInstanceService lmsDocumentInstanceService;

  @MockitoBean
  private ApplicationLetterService applicationLetterService;

  @MockitoBean
  private ApplicationService applicationService;

  @MockitoBean
  private DocumentLinkingService documentLinkingService;

  @MockitoBean
  private LicenceApplication application;

  @MockitoBean
  private DocumentInstanceSectionsSummaryView sectionsSummaryView;

  @MockitoBean
  private ApplicationLetterValidationService applicationLetterValidationService;

  private LicenceContinuationApplicationDetail continuationApplicationDetail;

  @AuthorisationSecurityTest
  void renderEditLetterOverview_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(null, null)))
                .with(csrf())
        )
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderEditLetterOverview_whenValid_rendersCorrectly() throws Exception {
    var appType = ApplicationType.CONTINUATION_APPLICATION;
    var appId = UUID.randomUUID();
    var documentInstance = DocumentInstanceDtoTestUtil.newBuilder().withTitle("Test Title").build();
    continuationApplicationDetail = new LicenceContinuationApplicationDetail();
    continuationApplicationDetail.setId(appId);
    continuationApplicationDetail.setStatus(LicenceContinuationApplicationStatus.ISSUE_DECISION);

    when(applicationService.getApplication(appType, appId)).thenReturn(application);
    when(applicationLetterService.getDocumentInstance(application)).thenReturn(documentInstance);
    when(lmsDocumentInstanceService.getDocumentInstanceSectionsSummaryView(documentInstance, true, application))
        .thenReturn(sectionsSummaryView);
    when(documentLinkingService.getApplicationCompanyNameFromDto(documentInstance)).thenReturn("Company Name");
    when(licenceContinuationService.getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(appId))
        .thenReturn(continuationApplicationDetail);
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.REGULATIONS_LICENSING, Set.of(Role.CONTINUATION_ISSUER)))
        .thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(appType, appId)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/letter/editLetterOverview"))
        .andExpect(model().attribute("documentInstanceDto", documentInstance))
        .andExpect(model().attribute("pageTitle", "Test Title for Company Name"))
        .andExpect(model().attributeExists("accordionId"))
        .andExpect(model().attribute("documentInstanceSectionsSummaryView", sectionsSummaryView))
        .andExpect(model().attribute("reloadUrl", ReverseRouter.route(on(ApplicationDocumentActionsController.class).renderReloadDocumentPage(appType ,appId, documentInstance.id()))))
        .andExpect(model().attribute("previewUrl", ReverseRouter.route(on(ApplicationDocumentActionsController.class).renderPreviewPdf(appType ,appId, documentInstance.id(), null))));
  }
}
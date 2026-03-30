package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceNotFoundException;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentLinkingService;
import uk.co.nstauthority.licensingmanagementservice.document.instance.LmsDocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.LmsPdfRenderResult;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.AuthorisationSecurityTest;

@WebMvcTest(ApplicationDocumentActionsController.class)
@ContextConfiguration(classes = ApplicationDocumentActionsController.class)
class ApplicationDocumentActionsControllerTest extends AbstractControllerTest {

  @MockitoBean
  private DocumentInstanceService documentInstanceService;

  @MockitoBean
  private LmsDocumentInstanceService lmsDocumentInstanceService;

  @MockitoBean
  private ApplicationService applicationService;

  @MockitoBean
  private DocumentLinkingService documentLinkingService;

  @MockitoBean
  private IssueLettersService issueLettersService;

  @Mock
  private DocumentInstanceDto documentInstanceMock;

  @Mock
  private DocumentTemplateDto documentTemplateMock;

  @Mock
  private LicenceApplication licenceApplication;

  @Mock
  private LmsPdfRenderResult lmsPdfRenderResult;

  private final UUID applicationId = UUID.randomUUID();
  private final UUID documentInstanceId = UUID.randomUUID();
  private final ApplicationType applicationType = ApplicationType.values()[0];

  @BeforeEach
  void setUp() {
    when(documentInstanceMock.documentTemplateDto()).thenReturn(documentTemplateMock);
    when(documentTemplateMock.title()).thenReturn("Test Letter Template");
    when(documentInstanceMock.title()).thenReturn("Test Letter Template");
  }

  @AuthorisationSecurityTest
  void renderPreviewPdf_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationDocumentActionsController.class).renderPreviewPdf(applicationType, applicationId, documentInstanceId, null)))
        )
        .andExpect(redirectionToLoginUrl());
  }

  @AuthorisationSecurityTest
  void renderReloadDocumentPage_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationDocumentActionsController.class).renderReloadDocumentPage(applicationType, applicationId, documentInstanceId)))
        )
        .andExpect(redirectionToLoginUrl());
  }

  @AuthorisationSecurityTest
  void reloadDocument_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
        post(ReverseRouter.route(on(ApplicationDocumentActionsController.class).reloadDocument(applicationType, applicationId, documentInstanceId, null)))
            .with(csrf())
    ).andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderPreviewPdf_redirectsToRenderPdf() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationDocumentActionsController.class).renderPreviewPdf(applicationType, applicationId, documentInstanceId, null )))
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(ApplicationDocumentActionsController.class).renderGeneratedPdf(applicationType, applicationId, documentInstanceId, null))));
  }

  @Test
  void renderGeneratedPdf_whenNoInstance_assertNotFound() throws Exception {
    when(documentInstanceService.getDocumentInstanceDtoOrThrow(documentInstanceId))
        .thenThrow(DocumentInstanceNotFoundException.class);

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationDocumentActionsController.class).renderGeneratedPdf(applicationType, applicationId, documentInstanceId, null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void renderGeneratedPdf_assertOk() throws Exception {
    when(applicationService.getApplication(applicationType, applicationId)).thenReturn(licenceApplication);
    when(documentInstanceService.getDocumentInstanceDtoOrThrow(documentInstanceId)).thenReturn(documentInstanceMock);

    var summaryViews = new DocumentInstanceSectionsSummaryView(List.of(), List.of(), Map.of());
    when(lmsDocumentInstanceService.getDocumentInstanceSectionsSummaryView(documentInstanceMock, false, licenceApplication))
        .thenReturn(summaryViews);

    var pdfBytes = new byte[]{1, 2, 3};
    when(lmsPdfRenderResult.pdfContent()).thenReturn(new ByteArrayResource(pdfBytes));

    when(lmsDocumentInstanceService.renderAndSignPdf(
        licenceApplication,
        true,
        documentInstanceMock,
        summaryViews.topLevelDocumentInstanceSectionSummaryViews(),
        regulatorUser
    )).thenReturn(lmsPdfRenderResult);

    var expectedFileName = "PREVIEW Test Letter Template.pdf";

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationDocumentActionsController.class).renderGeneratedPdf(applicationType, applicationId, documentInstanceId, null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(content().bytes(pdfBytes))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(expectedFileName)));
  }

  @Test
  void renderReloadDocumentPage_whenNoInstance_assertNotFound() throws Exception {
    when(documentInstanceService.getDocumentInstanceDtoOrThrow(documentInstanceId))
        .thenThrow(DocumentInstanceNotFoundException.class);

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationDocumentActionsController.class).renderReloadDocumentPage(applicationType, applicationId, documentInstanceId)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void renderReloadDocumentPage_assertOk() throws Exception {
    when(documentInstanceService.getDocumentInstanceDtoOrThrow(documentInstanceId)).thenReturn(documentInstanceMock);

    var companyName = "Test Company Ltd";
    when(documentLinkingService.getApplicationCompanyNameFromDto(documentInstanceMock)).thenReturn(companyName);

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationDocumentActionsController.class).renderReloadDocumentPage(applicationType, applicationId, documentInstanceId))).with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/letter/reloadDocumentInstance"))
        .andExpect(model().attribute("documentTitle", "Test Letter Template"))
        .andExpect(model().attribute("companyName", companyName))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(applicationType, applicationId))));
  }

  @Test
  void reloadDocument_whenNoInstance_assertNotFound() throws Exception {
    when(documentInstanceService.getDocumentInstanceDtoOrThrow(documentInstanceId))
        .thenThrow(DocumentInstanceNotFoundException.class);

    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationDocumentActionsController.class).reloadDocument(applicationType, applicationId, documentInstanceId, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void reloadDocument_assertRedirected() throws Exception {
    when(documentInstanceService.getDocumentInstanceDtoOrThrow(documentInstanceId)).thenReturn(documentInstanceMock);

    mockMvc.perform(
        post(ReverseRouter.route(on(ApplicationDocumentActionsController.class).reloadDocument(applicationType, applicationId, documentInstanceId, null)))
                     .with(user(regulatorUser))
                     .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(applicationType, applicationId))));

    verify(documentInstanceService).reloadDocumentInstance(documentInstanceMock);
  }

  @AuthorisationSecurityTest
  void approveAndSignDocument_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationDocumentActionsController.class).approveAndSignDocument(applicationType, applicationId, documentInstanceId, null, regulatorUser)))
        )
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void approveAndSignDocument_whenInstanceNotFound_assertNotFound() throws Exception {
    when(documentInstanceService.getDocumentInstanceDtoOrThrow(documentInstanceId))
        .thenThrow(DocumentInstanceNotFoundException.class);

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationDocumentActionsController.class).approveAndSignDocument(applicationType, applicationId, documentInstanceId, null, regulatorUser)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void approveAndSignDocument_assertRedirectsAndSavesLetter() throws Exception {
    when(applicationService.getApplication(applicationType, applicationId))
        .thenReturn(licenceApplication);

    when(documentInstanceService.getDocumentInstanceDtoOrThrow(documentInstanceId))
        .thenReturn(documentInstanceMock);

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationDocumentActionsController.class).approveAndSignDocument(applicationType, applicationId, documentInstanceId, null, regulatorUser)))
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(applicationType, applicationId))));

    verify(applicationService)
        .getApplication(applicationType, applicationId);

    verify(documentInstanceService)
        .getDocumentInstanceDtoOrThrow(documentInstanceId);

    verify(issueLettersService).saveApplicationLetterToS3(
        documentInstanceMock,
        licenceApplication,
        regulatorUser,
        false,
        lmsDocumentInstanceService
    );
  }
}
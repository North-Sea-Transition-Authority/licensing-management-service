package uk.co.nstauthority.licensingmanagementservice.licence.continuation.letter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceNotFoundException;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentItemType;
import uk.co.nstauthority.licensingmanagementservice.document.instance.LmsDocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.file.FileUsageType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.IssueLettersService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = ContinuationApplicationDocumentActionsController.class)
public class ContinuationApplicationDocumentActionsControllerTest extends AbstractControllerTest {

  public static final UUID applicationId = UUID.randomUUID();
  private static final UUID documentInstanceId = UUID.randomUUID();
  private static final ApplicationType applicationType = ApplicationType.CONTINUATION_APPLICATION;

  @MockitoBean
  private ApplicationService applicationService;

  @MockitoBean
  private DocumentInstanceService documentInstanceService;

  @MockitoBean
  private LmsDocumentInstanceService lmsDocumentInstanceService;

  @MockitoBean
  private IssueLettersService issueLettersService;

  @Mock
  private LicenceApplication licenceApplication;

  @Mock
  private DocumentInstanceDto documentInstanceMock;

  @Mock
  private LicenceContinuationApplicationDetail continuationApplicationDetail;

  @BeforeEach
  void setUp() {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);
    when(teamQueryService.userHasRoleInTeamType(any(), any(), any())).thenReturn(true);
    when(licenceContinuationService.getDetailByIdOrThrow(any())).thenReturn(continuationApplicationDetail);
    when(licenceContinuationService.getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(any())).thenReturn(continuationApplicationDetail);
    when(continuationApplicationDetail.getStatus()).thenReturn(LicenceContinuationApplicationStatus.ISSUE_DECISION);
  }

  @Test
  void approveAndSignDocument_whenInstanceNotFound_assertNotFound() throws Exception {
    when(documentInstanceService.getDocumentInstanceDtoOrThrow(documentInstanceId))
        .thenThrow(DocumentInstanceNotFoundException.class);

    mockMvc.perform(
            post(ReverseRouter.route(on(ContinuationApplicationDocumentActionsController.class).approveAndSignDocument(applicationType, applicationId, documentInstanceId, null, regulatorUser)))
                .with(csrf())
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
            post(ReverseRouter.route(on(ContinuationApplicationDocumentActionsController.class).approveAndSignDocument(applicationType, applicationId, documentInstanceId, null, regulatorUser)))
                .with(csrf())
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))));

    verify(applicationService).getApplication(applicationType, applicationId);
    verify(documentInstanceService).getDocumentInstanceDtoOrThrow(documentInstanceId);

    verify(issueLettersService).saveApplicationLetterToS3(
        documentInstanceMock,
        licenceApplication,
        regulatorUser,
        false,
        lmsDocumentInstanceService,
        FileUsageType.APPLICATION_CONTINUATION_LETTER.getUsageType(),
        DocumentItemType.CONTINUATION_LETTER.name()
    );

    verify(licenceContinuationService).issueContinuationLetter(licenceApplication);
  }
}
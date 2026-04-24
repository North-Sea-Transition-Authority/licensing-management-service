package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionForm;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionFormValidator;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionNotFoundException;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldViewService;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption;
import uk.co.nstauthority.licensingmanagementservice.document.instance.DocumentInstanceSectionDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@WebMvcTest(ApplicationLetterDocumentController.class)
@ContextConfiguration(classes = ApplicationLetterDocumentController.class)
class ApplicationLetterDocumentControllerTest extends AbstractControllerTest {

  private static final ApplicationType APP_TYPE = ApplicationType.CONTINUATION_APPLICATION;
  private static final UUID APP_ID = UUID.randomUUID();
  private static final UUID SECTION_ID = UUID.randomUUID();
  private static final int DISPLAY_ORDER = 2;

  private DocumentInstanceSectionDto documentInstanceSectionDto;
  private List<DocumentMailMergeFieldView> applicableDocumentMailMergeFieldViews;

  @MockitoBean
  private DocumentInstanceSectionService documentInstanceSectionService;

  @MockitoBean
  private DocumentMailMergeFieldViewService documentMailMergeFieldViewService;

  @MockitoBean
  private DocumentInstanceSectionFormValidator documentInstanceSectionFormValidator;

  @MockitoBean
  private ApplicationLetterService applicationLetterService;

  @MockitoBean
  private ApplicationService applicationService;

  @MockitoBean
  private LicenceApplication application;

  @MockitoBean
  private ApplicationLetterValidationService  applicationLetterValidationService;

  private LicenceContinuationApplicationDetail continuationApplicationDetail;

  @BeforeEach
  void setUp() {
    when(application.getId()).thenReturn(APP_ID);
    when(application.getApplicationType()).thenReturn(APP_TYPE);
    when(applicationService.getApplication(APP_TYPE, APP_ID)).thenReturn(application);

    var documentInstance = DocumentInstanceDtoTestUtil.newBuilder().build();
    documentInstanceSectionDto = DocumentInstanceSectionDtoTestUtil
        .newBuilder()
        .withId(SECTION_ID)
        .withDocumentInstanceDto(documentInstance)
        .withTitle("My Section")
        .withDisplayOrder(DISPLAY_ORDER)
        .build();

    var bindingResult = new BeanPropertyBindingResult(DocumentInstanceSectionForm.from(documentInstanceSectionDto), "form");
    when(applicationLetterValidationService.getDocumentSectionSpecificErrors(any(),any())).thenReturn(bindingResult);

    applicableDocumentMailMergeFieldViews = List.of(
        new DocumentMailMergeFieldView("MNEMONIC_1", "Desc 1"),
        new DocumentMailMergeFieldView("MNEMONIC_2", "Desc 2")
    );

    continuationApplicationDetail = new LicenceContinuationApplicationDetail();
    continuationApplicationDetail.setId(APP_ID);
    continuationApplicationDetail.setStatus(LicenceContinuationApplicationStatus.ISSUE_DECISION);

    when(licenceContinuationService.getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(APP_ID))
        .thenReturn(continuationApplicationDetail);

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.REGULATIONS_LICENSING, Set.of(Role.CONTINUATION_ISSUER))).thenReturn(true);
  }


  @Test
  void renderAddSectionPage_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderAddSectionPage(APP_TYPE, APP_ID, SECTION_ID, AddSectionOption.ADD_SUBSECTION)))
        )
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void createSection_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).createSection(APP_TYPE, APP_ID, SECTION_ID, AddSectionOption.ADD_SUBSECTION, null, null)))
                .with(csrf())
        )
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderEditSectionPage_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderEditSectionPage(APP_TYPE, APP_ID, SECTION_ID)))
        )
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void updateSection_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).updateSection(APP_TYPE, APP_ID, SECTION_ID, null, null))).with(csrf())
        )
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderRemoveSectionPage_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderRemoveSectionPage(APP_TYPE, APP_ID, SECTION_ID)))
        )
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void removeSectionPage_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).removeSectionPage(APP_TYPE, APP_ID, SECTION_ID)))
                .with(csrf())
        )
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderAddSectionPage_whenSectionNotFound_throwNotFound() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenThrow(DocumentInstanceSectionNotFoundException.class);

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderAddSectionPage(APP_TYPE, APP_ID, SECTION_ID, AddSectionOption.ADD_SUBSECTION)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void createSection_whenSectionNotFound_throwNotFound() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenThrow(DocumentInstanceSectionNotFoundException.class);

    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).createSection(APP_TYPE, APP_ID, SECTION_ID, AddSectionOption.ADD_SUBSECTION, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void renderEditSectionPage_whenSectionNotFound_throwNotFound() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenThrow(DocumentInstanceSectionNotFoundException.class);

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderEditSectionPage(APP_TYPE, APP_ID, SECTION_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void updateSection_whenSectionNotFound_throwNotFound() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenThrow(DocumentInstanceSectionNotFoundException.class);

    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).updateSection(APP_TYPE, APP_ID, SECTION_ID, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void renderRemoveSectionPage_whenSectionNotFound_throwNotFound() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenThrow(DocumentInstanceSectionNotFoundException.class);

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderRemoveSectionPage(APP_TYPE, APP_ID, SECTION_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void removeSectionPage_whenSectionNotFound_throwNotFound() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenThrow(DocumentInstanceSectionNotFoundException.class);

    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).removeSectionPage(APP_TYPE, APP_ID, SECTION_ID)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isNotFound());
  }

  @Test
  void renderRemoveSectionPage_whenLastSection_throwsForbidden() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenReturn(documentInstanceSectionDto);
    when(documentInstanceSectionService.getTopLevelDocumentInstanceSectionDtos(documentInstanceSectionDto.documentInstanceDto()))
        .thenReturn(List.of(documentInstanceSectionDto));

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderRemoveSectionPage(APP_TYPE, APP_ID, SECTION_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void removeSectionPage_whenLastSection_throwsForbidden() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenReturn(documentInstanceSectionDto);
    when(documentInstanceSectionService.getTopLevelDocumentInstanceSectionDtos(documentInstanceSectionDto.documentInstanceDto()))
        .thenReturn(List.of(documentInstanceSectionDto));

    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).removeSectionPage(APP_TYPE, APP_ID, SECTION_ID)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(documentInstanceSectionService, never()).deleteDocumentInstanceSection(any());
  }

  @Test
  void renderAddSectionPage_returnsCorrectView() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenReturn(documentInstanceSectionDto);
    when(documentMailMergeFieldViewService.getApplicableDocumentMailMergeFieldViews(
        documentInstanceSectionDto.documentInstanceDto().documentTemplateDto()))
        .thenReturn(applicableDocumentMailMergeFieldViews);

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderAddSectionPage(APP_TYPE, APP_ID, SECTION_ID, AddSectionOption.ADD_SUBSECTION)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/documents/addOrEditLetterSection"))
        .andExpect(model().attribute("documentInstanceSectionDto", documentInstanceSectionDto))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("pageTitle", "Add subsection"))
        .andExpect(model().attribute("mailMergeFieldViews", applicableDocumentMailMergeFieldViews))
        .andExpect(model().attributeExists("breadcrumbs"))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(APP_TYPE, APP_ID))));
  }

  @Test
  void createSection_whenInvalid_returnsToForm() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenReturn(documentInstanceSectionDto);
    when(documentMailMergeFieldViewService.getApplicableDocumentMailMergeFieldViews(
        documentInstanceSectionDto.documentInstanceDto().documentTemplateDto()))
        .thenReturn(applicableDocumentMailMergeFieldViews);

    doAnswer(invocation -> {
      var bindingResult = invocation.getArgument(2, BindingResult.class);
      bindingResult.rejectValue("content", "mandatory", "validation message");
      return null;
    }).when(documentInstanceSectionFormValidator).validate(any(), eq(documentInstanceSectionDto.documentInstanceDto()), any());

    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).createSection(APP_TYPE, APP_ID, SECTION_ID, AddSectionOption.ADD_SUBSECTION, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/documents/addOrEditLetterSection"))
        .andExpect(model().attributeHasFieldErrors("form", "content"))
        .andExpect(model().attribute("documentInstanceSectionDto", documentInstanceSectionDto))
        .andExpect(model().attribute("pageTitle", "Add subsection"))
        .andExpect(model().attribute("mailMergeFieldViews", applicableDocumentMailMergeFieldViews));

    verify(documentInstanceSectionService, never()).createDocumentInstanceSection(any(), any(), any(), anyInt());
  }

  @Test
  void createSection_whenValid_redirects() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenReturn(documentInstanceSectionDto);
    when(applicationLetterService.getParentDocumentSectionDto(AddSectionOption.ADD_SUBSECTION, documentInstanceSectionDto))
        .thenReturn(documentInstanceSectionDto);

    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).createSection(APP_TYPE, APP_ID, SECTION_ID, AddSectionOption.ADD_SUBSECTION, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(APP_TYPE, APP_ID))));

    verify(documentInstanceSectionService).createDocumentInstanceSection(
        eq(documentInstanceSectionDto.documentInstanceDto()),
        eq(documentInstanceSectionDto),
        any(DocumentInstanceSectionForm.class),
        eq(AddSectionOption.getDisplayOrder(AddSectionOption.ADD_SUBSECTION, DISPLAY_ORDER))
    );
  }

  @Test
  void createSection_whenNoParentDocumentSectionFound_throwsNotFound() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenReturn(documentInstanceSectionDto);
    when(applicationLetterService.getParentDocumentSectionDto(AddSectionOption.ADD_SUBSECTION, documentInstanceSectionDto))
        .thenThrow(DocumentInstanceSectionNotFoundException.class);

    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).createSection(APP_TYPE, APP_ID, SECTION_ID, AddSectionOption.ADD_SUBSECTION, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Cannot find document section with id %s".formatted(SECTION_ID)));

    verify(documentInstanceSectionService, never()).createDocumentInstanceSection(any(), any(), any(), anyInt());
  }

  @Test
  void renderEditSectionPage_returnsCorrectView() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenReturn(documentInstanceSectionDto);
    when(documentMailMergeFieldViewService.getApplicableDocumentMailMergeFieldViews(
        documentInstanceSectionDto.documentInstanceDto().documentTemplateDto()))
        .thenReturn(applicableDocumentMailMergeFieldViews);

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderEditSectionPage(APP_TYPE, APP_ID, SECTION_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/documents/addOrEditLetterSection"))
        .andExpect(model().attribute("documentInstanceSectionDto", documentInstanceSectionDto))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("pageTitle", "Edit My Section"))
        .andExpect(model().attribute("mailMergeFieldViews", applicableDocumentMailMergeFieldViews))
        .andExpect(model().attributeExists("breadcrumbs"))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(APP_TYPE, APP_ID))));
  }

  @Test
  void updateSection_whenInvalid_returnsToForm() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenReturn(documentInstanceSectionDto);
    when(documentMailMergeFieldViewService.getApplicableDocumentMailMergeFieldViews(
        documentInstanceSectionDto.documentInstanceDto().documentTemplateDto()))
        .thenReturn(applicableDocumentMailMergeFieldViews);

    doAnswer(invocation -> {
      var bindingResult = invocation.getArgument(2, BindingResult.class);
      bindingResult.rejectValue("content", "mandatory", "validation message");
      return null;
    }).when(documentInstanceSectionFormValidator).validate(any(), eq(documentInstanceSectionDto.documentInstanceDto()), any());

    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).updateSection(APP_TYPE, APP_ID, SECTION_ID, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/documents/addOrEditLetterSection"))
        .andExpect(model().attributeHasFieldErrors("form", "content"))
        .andExpect(model().attribute("documentInstanceSectionDto", documentInstanceSectionDto))
        .andExpect(model().attribute("pageTitle", "Edit My Section"))
        .andExpect(model().attribute("mailMergeFieldViews", applicableDocumentMailMergeFieldViews));

    verify(documentInstanceSectionService, never()).editDocumentInstanceSection(any(), any());
  }

  @Test
  void updateSection_whenValid_redirects() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenReturn(documentInstanceSectionDto);

    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).updateSection(APP_TYPE, APP_ID, SECTION_ID, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(APP_TYPE, APP_ID))));

    verify(documentInstanceSectionService).editDocumentInstanceSection(eq(documentInstanceSectionDto), any(DocumentInstanceSectionForm.class));
  }

  @Test
  void renderRemoveSectionPage_returnsCorrectView() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenReturn(documentInstanceSectionDto);
    when(documentInstanceSectionService.getTopLevelDocumentInstanceSectionDtos(documentInstanceSectionDto.documentInstanceDto()))
        .thenReturn(List.of(documentInstanceSectionDto, DocumentInstanceSectionDtoTestUtil.newBuilder().build()));

    mockMvc.perform(
            get(ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderRemoveSectionPage(APP_TYPE, APP_ID, SECTION_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/documents/removeLetterSection"))
        .andExpect(model().attribute("documentSectionDto", documentInstanceSectionDto))
        .andExpect(model().attributeExists("breadcrumbs"))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(ApplicationLetterController.class)
                                                                          .renderEditLetterOverview(APP_TYPE, APP_ID))));
  }

  @Test
  void removeSectionPage_whenValid_redirects() throws Exception {
    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(SECTION_ID))
        .thenReturn(documentInstanceSectionDto);
    when(documentInstanceSectionService.getTopLevelDocumentInstanceSectionDtos(documentInstanceSectionDto.documentInstanceDto()))
        .thenReturn(List.of(documentInstanceSectionDto, DocumentInstanceSectionDtoTestUtil.newBuilder().build()));

    mockMvc.perform(
            post(ReverseRouter.route(on(ApplicationLetterDocumentController.class).removeSectionPage(APP_TYPE, APP_ID, SECTION_ID)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(APP_TYPE, APP_ID))));

    verify(documentInstanceSectionService).deleteDocumentInstanceSection(documentInstanceSectionDto);
  }
}
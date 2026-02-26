package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates;

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
import static uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption.ADD_AFTER;
import static uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption.ADD_BEFORE;
import static uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption.ADD_SUBSECTION;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldViewService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionConditionService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionForm;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionNotFoundException;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionService;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.search.DocumentTemplateSearchController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.AuthorisationSecurityTest;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = DocumentTemplateSectionController.class)
class DocumentTemplateSectionControllerTest extends AbstractControllerTest {

  private static final int DISPLAY_ORDER = 2;
  private static final String TITLE = "title";

  @MockitoBean
  private DocumentTemplateSectionService documentTemplateSectionService;

  @MockitoBean
  private DocumentMailMergeFieldViewService documentMailMergeFieldViewService;

  @MockitoBean
  private LmsDocumentTemplateSectionFormValidator documentTemplateSectionFormValidator;

  @MockitoBean
  private DocumentTemplateSectionConditionService documentTemplateSectionConditionService;

  public static final UUID DOCUMENT_TEMPLATE_ID = UUID.randomUUID();
  private static final UUID DOCUMENT_SECTION_ID = UUID.randomUUID();
  private static DocumentTemplateSectionDto documentSection;
  public static DocumentTemplateDto documentTemplateDto;

  @BeforeEach
  void setUp() {
    regulatorUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(REGULATOR_USER_WUA_ID)
        .build();

    when(teamManagementService.getTeamTypesUserIsMemberOf(regulatorUser.wuaId()))
        .thenReturn(Set.of(TeamType.LICENCE_MANAGEMENT));

    documentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder()
        .withId(DOCUMENT_TEMPLATE_ID)
        .build();

    documentSection = DocumentTemplateSectionDtoTestUtil.newBuilder()
        .withId(DOCUMENT_SECTION_ID)
        .withDisplayOrder(DISPLAY_ORDER)
        .withTitle(TITLE)
        .withDocumentTemplateDto(documentTemplateDto)
        .build();
  }

  @AuthorisationSecurityTest
  void renderCreateSectionPage_whenNotLoggedIn_thenRedirectToLogin() throws Exception {
    mockMvc.perform(
        get(ReverseRouter.route(on(DocumentTemplateSectionController.class).renderAddSectionPage(UUID.randomUUID(), null)))
        )
        .andExpect(redirectionToLoginUrl());
  }

  @AuthorisationSecurityTest
  void createSection_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
        post(ReverseRouter.route(on(DocumentTemplateSectionController.class).createSection(UUID.randomUUID(), null, null, null)))
                        .with(csrf())
        )
        .andExpect(redirectionToLoginUrl());
  }

  @AuthorisationSecurityTest
  void renderEditSectionPage_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
        get(ReverseRouter.route(on(DocumentTemplateSectionController.class).renderEditSectionPage(UUID.randomUUID())))
        )
        .andExpect(redirectionToLoginUrl());
  }

  @AuthorisationSecurityTest
  void updateSection_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
        post(ReverseRouter.route(on(DocumentTemplateSectionController.class).updateSection(UUID.randomUUID(), null, null)))
                        .with(csrf())
        )
        .andExpect(redirectionToLoginUrl());
  }

  @AuthorisationSecurityTest
  void renderRemoveSectionPage_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
        get(ReverseRouter.route(on(DocumentTemplateSectionController.class).renderRemoveSectionPage(UUID.randomUUID())))
        )
        .andExpect(redirectionToLoginUrl());
  }

  @AuthorisationSecurityTest
  void removeSectionPage_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
        post(ReverseRouter.route(on(DocumentTemplateSectionController.class).removeSectionPage(UUID.randomUUID(), null, null)))
                        .with(csrf())
        )
        .andExpect(redirectionToLoginUrl());
  }

  @SecurityTest
  void renderRemoveSectionPage_whenOnly1SectionLeft_throwForbidden() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenReturn(documentSection);
    when(documentTemplateSectionService.getTopLevelDocumentTemplateSectionDtos(documentTemplateDto))
        .thenReturn(List.of(documentSection));

    mockMvc.perform(
        get(ReverseRouter.route(on(DocumentTemplateSectionController.class).renderRemoveSectionPage(DOCUMENT_SECTION_ID)))
                        .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            "Cannot remove last section with id %s from document template, there must be at least 1 section per template".formatted(
                DOCUMENT_SECTION_ID)
        ));
  }

  @SecurityTest
  void renderAddSectionPage_whenNoTemplateExists_throwNotFound() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenThrow(DocumentTemplateSectionNotFoundException.class);

    mockMvc.perform(
        get(ReverseRouter.route(on(DocumentTemplateSectionController.class).renderAddSectionPage(DOCUMENT_SECTION_ID, ADD_BEFORE)))
                        .with(user(regulatorUser))
        )
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Cannot find document section with id %s".formatted(DOCUMENT_SECTION_ID)));
  }

  @SecurityTest
  void createSection_whenNoTemplateExists_throwNotFound() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenThrow(DocumentTemplateSectionNotFoundException.class);

    mockMvc.perform(
        post(ReverseRouter.route(on(DocumentTemplateSectionController.class).createSection(DOCUMENT_SECTION_ID, ADD_BEFORE, null, null)))
                     .with(user(regulatorUser))
                     .with(csrf())
        )
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Cannot find document section with id %s".formatted(DOCUMENT_SECTION_ID)));
  }

  @SecurityTest
  void renderEditSectionPage_whenNoTemplateExists_throwNotFound() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenThrow(DocumentTemplateSectionNotFoundException.class);

    mockMvc.perform(
            get(ReverseRouter.route(on(DocumentTemplateSectionController.class).renderEditSectionPage(DOCUMENT_SECTION_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Cannot find document section with id %s".formatted(DOCUMENT_SECTION_ID)));
  }

  @SecurityTest
  void updateSection_whenNoTemplateExists_throwNotFound() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID)).thenThrow(
        DocumentTemplateSectionNotFoundException.class);

    mockMvc.perform(
        post(ReverseRouter.route(on(DocumentTemplateSectionController.class).updateSection(DOCUMENT_SECTION_ID, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Cannot find document section with id %s".formatted(DOCUMENT_SECTION_ID)));
  }

  @SecurityTest
  void renderRemoveSectionPage_whenNoTemplateExists_throwNotFound() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID)).thenThrow(
        DocumentTemplateSectionNotFoundException.class);

    mockMvc.perform(
        get(ReverseRouter.route(on(DocumentTemplateSectionController.class).renderRemoveSectionPage(DOCUMENT_SECTION_ID)))
                        .with(user(regulatorUser))
        )
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Cannot find document section with id %s".formatted(DOCUMENT_SECTION_ID)));
  }

  @SecurityTest
  void removeSectionPage_whenOnly1SectionLeft_throwForbidden() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenReturn(documentSection);

    when(documentTemplateSectionService.getTopLevelDocumentTemplateSectionDtos(documentTemplateDto))
        .thenReturn(List.of(documentSection));

    mockMvc.perform(
        post(ReverseRouter.route(on(DocumentTemplateSectionController.class).removeSectionPage(DOCUMENT_SECTION_ID, null, null)))
                     .with(user(regulatorUser))
                     .with(csrf())
        )
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            "Cannot remove last section with id %s from document template, there must be at least 1 section per template".formatted(
                DOCUMENT_SECTION_ID)));
  }

  @SecurityTest
  void removeSectionPage_whenNoTemplateExists_throwNotFound() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID)).thenThrow(
        DocumentTemplateSectionNotFoundException.class);

    mockMvc.perform(
        post(ReverseRouter.route(on(DocumentTemplateSectionController.class).removeSectionPage(DOCUMENT_SECTION_ID, null, null)))
                     .with(user(regulatorUser))
                     .with(csrf())
        )
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Cannot find document section with id %s".formatted(DOCUMENT_SECTION_ID)));
  }

  @ParameterizedTest
  @MethodSource("getAddOptionToPageName")
  void renderAddSectionPage(AddSectionOption addSectionOption, String pageTitle) throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenReturn(documentSection);

    var applicableDocumentMailMergeFieldViews = List.of(
        new DocumentMailMergeFieldView("TEST_MNEMONIC_1", "Test description 1"),
        new DocumentMailMergeFieldView("TEST_MNEMONIC_2", "Test description 2")
    );

    when(documentMailMergeFieldViewService.getApplicableDocumentMailMergeFieldViews(documentTemplateDto))
        .thenReturn(applicableDocumentMailMergeFieldViews);

    var conditionsMap = Map.of("MNEMONIC", "TITLE");
    when(documentTemplateSectionConditionService.getConditionsFdsSelectMap(documentTemplateDto))
        .thenReturn(conditionsMap);

    mockMvc.perform(
        get(ReverseRouter.route(on(DocumentTemplateSectionController.class).renderAddSectionPage(DOCUMENT_SECTION_ID, addSectionOption)))
                        .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/document/sections/addOrEditSection"))
        .andExpect(model().attribute("documentTemplateSectionDto", documentSection))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("mailMergeFieldViews", applicableDocumentMailMergeFieldViews))
        .andExpect(model().attribute("currentPage", pageTitle))
        .andExpect(model().attribute("pageTitle", pageTitle))
        .andExpect(model().attribute(
            "breadcrumbs",
            Map.of(
                ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(DOCUMENT_TEMPLATE_ID, null)),
                "%s".formatted(documentTemplateDto.title()),
                ReverseRouter.route(on(DocumentTemplateSearchController.class).renderDocumentTemplateSearch(null, null, null)),
                "Document library"
            )
        ))
        .andExpect(model().attribute("conditionsFdsSelectMap", conditionsMap));
  }

  @ParameterizedTest
  @MethodSource("getAddOptionToPageName")
  void createSection_whenInvalid_assertOk(AddSectionOption addSectionOption, String pageTitle) throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenReturn(documentSection);

    doAnswer(invocation -> {
      var bindingResult = invocation.getArgument(1, BindingResult.class);
      bindingResult.rejectValue("content", "mandatory", "validation message");
      return null;
    })
        .when(documentTemplateSectionFormValidator)
        .validate(any(LmsDocumentTemplateSectionForm.class), any(BindingResult.class), eq(documentTemplateDto));

    var applicableDocumentMailMergeFieldViews = List.of(
        new DocumentMailMergeFieldView("TEST_MNEMONIC_1", "Test description 1"),
        new DocumentMailMergeFieldView("TEST_MNEMONIC_2", "Test description 2")
    );

    when(documentMailMergeFieldViewService.getApplicableDocumentMailMergeFieldViews(documentTemplateDto))
        .thenReturn(applicableDocumentMailMergeFieldViews);

    var conditionsMap = Map.of("MNEMONIC", "TITLE");
    when(documentTemplateSectionConditionService.getConditionsFdsSelectMap(documentTemplateDto))
        .thenReturn(conditionsMap);

    mockMvc.perform(
        post(ReverseRouter.route(on(DocumentTemplateSectionController.class).createSection(DOCUMENT_SECTION_ID, addSectionOption, null, null)))
                     .with(user(regulatorUser))
                     .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/document/sections/addOrEditSection"))
        .andExpect(model().attribute("documentTemplateSectionDto", documentSection))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("mailMergeFieldViews", applicableDocumentMailMergeFieldViews))
        .andExpect(model().attribute("currentPage", pageTitle))
        .andExpect(model().attribute("pageTitle", pageTitle))
        .andExpect(model().attribute(
            "breadcrumbs",
            Map.of(
                ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(DOCUMENT_TEMPLATE_ID, null)),
                "%s".formatted(documentTemplateDto.title()),
                ReverseRouter.route(on(DocumentTemplateSearchController.class).renderDocumentTemplateSearch(null, null, null)),
                "Document library"
            )
        ))
        .andExpect(model().attribute("conditionsFdsSelectMap", conditionsMap));

    verify(documentTemplateSectionService, never()).createDocumentTemplateSection(any(), any(), any(), anyInt());
  }

  @ParameterizedTest
  @MethodSource("getAddOptionToDisplayOrder")
  void createSection_whenValid_assertRedirect(AddSectionOption addSectionOption, int displayOrder) throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenReturn(documentSection);
    var form = new LmsDocumentTemplateSectionForm();

    var parentDto = switch (addSectionOption) {
      case ADD_BEFORE,
           ADD_AFTER -> null;
      case ADD_SUBSECTION -> documentSection;
    };

    when(documentTemplateSectionService.createDocumentTemplateSection(documentTemplateDto, parentDto, form, displayOrder))
        .thenReturn(documentSection);

    mockMvc.perform(
        post(ReverseRouter.route(on(DocumentTemplateSectionController.class).createSection(DOCUMENT_SECTION_ID, addSectionOption, null, null)))
                     .with(user(regulatorUser))
                     .with(csrf())
                     .flashAttr("form", form)
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(DOCUMENT_TEMPLATE_ID, null))));

    verify(documentTemplateSectionService).createDocumentTemplateSection(documentTemplateDto, parentDto, form, displayOrder);
  }

  @Test
  void renderEditSectionPage() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenReturn(documentSection);

    var applicableDocumentMailMergeFieldViews = List.of(
        new DocumentMailMergeFieldView("TEST_MNEMONIC_1", "Test description 1"),
        new DocumentMailMergeFieldView("TEST_MNEMONIC_2", "Test description 2")
    );

    when(documentMailMergeFieldViewService.getApplicableDocumentMailMergeFieldViews(documentTemplateDto))
        .thenReturn(applicableDocumentMailMergeFieldViews);

    var conditionsMap = Map.of("MNEMONIC", "TITLE");
    when(documentTemplateSectionConditionService.getConditionsFdsSelectMap(documentTemplateDto))
        .thenReturn(conditionsMap);

    mockMvc.perform(
        get(ReverseRouter.route(on(DocumentTemplateSectionController.class).renderEditSectionPage(DOCUMENT_SECTION_ID)))
                        .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/document/sections/addOrEditSection"))
        .andExpect(model().attribute("documentTemplateSectionDto", documentSection))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("mailMergeFieldViews", applicableDocumentMailMergeFieldViews))
        .andExpect(model().attribute("currentPage", "Edit %s".formatted(documentSection.title())))
        .andExpect(model().attribute(
            "breadcrumbs",
            Map.of(
                ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(DOCUMENT_TEMPLATE_ID, null)),
                "%s".formatted(documentTemplateDto.title()),
                ReverseRouter.route(on(DocumentTemplateSearchController.class).renderDocumentTemplateSearch(null, null, null)),
                "Document library"
            )
        ))
        .andExpect(model().attribute("conditionsFdsSelectMap", conditionsMap));
  }

  @Test
  void updateSection_whenInValid_assertOk() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenReturn(documentSection);

    doAnswer(invocation -> {
      var bindingResult = invocation.getArgument(1, BindingResult.class);
      bindingResult.rejectValue("content", "mandatory", "validation message");
      return null;
    })
        .when(documentTemplateSectionFormValidator)
        .validate(any(LmsDocumentTemplateSectionForm.class), any(BindingResult.class), eq(documentTemplateDto));

    var applicableDocumentMailMergeFieldViews = List.of(
        new DocumentMailMergeFieldView("TEST_MNEMONIC_1", "Test description 1"),
        new DocumentMailMergeFieldView("TEST_MNEMONIC_2", "Test description 2")
    );

    when(documentMailMergeFieldViewService.getApplicableDocumentMailMergeFieldViews(documentTemplateDto))
        .thenReturn(applicableDocumentMailMergeFieldViews);

    var conditionsMap = Map.of("MNEMONIC", "TITLE");
    when(documentTemplateSectionConditionService.getConditionsFdsSelectMap(documentTemplateDto))
        .thenReturn(conditionsMap);

    mockMvc.perform(post(ReverseRouter.route(on(DocumentTemplateSectionController.class).updateSection(DOCUMENT_SECTION_ID, null, null)))
                .with(user(regulatorUser))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/document/sections/addOrEditSection"))
        .andExpect(model().attribute("documentTemplateSectionDto", documentSection))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("mailMergeFieldViews", applicableDocumentMailMergeFieldViews))
        .andExpect(model().attribute("currentPage", "Edit %s".formatted(documentSection.title())))
        .andExpect(model().attribute(
            "breadcrumbs",
            Map.of(
                ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(DOCUMENT_TEMPLATE_ID, null)),
                "%s".formatted(documentTemplateDto.title()),
                ReverseRouter.route(on(DocumentTemplateSearchController.class).renderDocumentTemplateSearch(null, null, null)),
                "Document library"
            )
        ))
        .andExpect(model().attribute("conditionsFdsSelectMap", conditionsMap));

    verify(documentTemplateSectionService, never()).editDocumentTemplateSection(any(), any());
  }

  @Test
  void updateSection_whenValid_assertRedirect() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenReturn(documentSection);

    mockMvc.perform(
            post(ReverseRouter.route(on(DocumentTemplateSectionController.class).updateSection(DOCUMENT_SECTION_ID, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(DOCUMENT_TEMPLATE_ID, null))));

    verify(documentTemplateSectionService).editDocumentTemplateSection(eq(documentSection), any(DocumentTemplateSectionForm.class));
  }

  @Test
  void renderRemoveSectionPage() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenReturn(documentSection);
    when(documentTemplateSectionService.getTopLevelDocumentTemplateSectionDtos(documentTemplateDto))
        .thenReturn(List.of(documentSection, DocumentTemplateSectionDtoTestUtil.newBuilder().build()));

    mockMvc.perform(
        get(ReverseRouter.route(on(DocumentTemplateSectionController.class).renderRemoveSectionPage(DOCUMENT_SECTION_ID)))
                        .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/document/sections/removeSection"))
        .andExpect(model().attribute("documentSectionDto", documentSection))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute(
            "cancelUrl",
            ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(documentSection.documentTemplateDto().id(), null))
        ))
        .andExpect(model().attribute("currentPage", "Remove %s".formatted(documentSection.title())))
        .andExpect(model().attribute(
            "breadcrumbs",
            Map.of(
                ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(DOCUMENT_TEMPLATE_ID, null)),
                "%s".formatted(documentSection.documentTemplateDto().title()),
                ReverseRouter.route(on(DocumentTemplateSearchController.class).renderDocumentTemplateSearch(null, null, null)),
                "Document library"
            )
        ));
  }

  @Test
  void removeSectionPage_whenValid_assertRedirect() throws Exception {
    givenUserIsRegulatorAndDocumentTemplateManager();
    when(documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(DOCUMENT_SECTION_ID))
        .thenReturn(documentSection);
    when(documentTemplateSectionService.getTopLevelDocumentTemplateSectionDtos(documentTemplateDto))
        .thenReturn(List.of(documentSection, DocumentTemplateSectionDtoTestUtil.newBuilder().build()));

    mockMvc.perform(
        post(ReverseRouter.route(on(DocumentTemplateSectionController.class).removeSectionPage(DOCUMENT_SECTION_ID, null, null)))
                     .with(user(regulatorUser))
                     .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(DOCUMENT_TEMPLATE_ID, null))));
  }

  private static Stream<Arguments> getAddOptionToPageName() {
    return Stream.of(
        Arguments.of(ADD_BEFORE, "Add section before %s".formatted(TITLE)),
        Arguments.of(ADD_AFTER, "Add section after %s".formatted(TITLE)),
        Arguments.of(ADD_SUBSECTION, "Add subsection")
    );
  }

  protected void givenUserIsRegulatorAndDocumentTemplateManager() {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(),TeamType.LICENCE_MANAGEMENT, Set.of(Role.DOCUMENT_TEMPLATE_MANAGER))).thenReturn(true);
  }

  private static Stream<Arguments> getAddOptionToDisplayOrder() {
    return Stream.of(
        Arguments.of(ADD_BEFORE, DISPLAY_ORDER),
        Arguments.of(ADD_AFTER, DISPLAY_ORDER + 1),
        Arguments.of(ADD_SUBSECTION, 1)
    );
  }
}
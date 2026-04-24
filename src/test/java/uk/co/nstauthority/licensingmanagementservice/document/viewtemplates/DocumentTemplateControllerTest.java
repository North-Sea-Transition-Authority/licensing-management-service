package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionSummaryView;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateMailMergeFieldFormatter;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateSectionUrlsTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.document.search.DocumentTemplateSearchController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = DocumentTemplateController.class)
class DocumentTemplateControllerTest extends AbstractControllerTest {

  private static final UUID DOCUMENT_TEMPLATE_ID = UUID.randomUUID();

  @MockitoBean
  LmsDocumentTemplateService lmsDocumentTemplateService;

  @MockitoBean
  DocumentTemplateMailMergeFieldFormatter documentTemplateMailMergeFieldFormatter;

  @Test
  void renderDocumentTemplateOverview_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(
            post(ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(null, null)))
                .with(csrf())
        )
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderTemplateOverview() throws Exception {
   DocumentTemplateDto documentTemplateDto = DocumentTemplateDtoTestUtil
        .newBuilder()
        .withId(DOCUMENT_TEMPLATE_ID)
        .build();

    var section = new DocumentTemplateSectionSummaryView(
        UUID.randomUUID(),
        "1",
        "Test title",
        "Test content",
        null,
        false,
        List.of(),
        Map.of(),
        DocumentTemplateSectionUrlsTestUtil.newBuilder().build(),
        List.of()
    );

    var documentSummaryView = DocumentTemplateSectionsSummaryViewTestUtil.newBuilder()
        .withDocumentTemplateSectionSummaryViews(List.of(section))
        .build();

    when(lmsDocumentTemplateService.getDocumentTemplateSectionsSummaryView(any(), any())).thenReturn(documentSummaryView);
    when(documentTemplateService.getDocumentTemplateDtoOrThrow(DOCUMENT_TEMPLATE_ID)).thenReturn(documentTemplateDto);

    mockMvc.perform(
            get(ReverseRouter.route(on(DocumentTemplateController.class)
                .renderTemplateOverview(DOCUMENT_TEMPLATE_ID, null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/document/templateOverview"))
        .andExpect(model().attribute("documentSectionsSummaryView", documentSummaryView))
        .andExpect(model().attribute("documentTemplateDto", documentTemplateDto))
        .andExpect(model().attribute("accordionId", DOCUMENT_TEMPLATE_ID))
        .andExpect(model().attribute("breadcrumbs", Map.of(ReverseRouter.route(on(DocumentTemplateSearchController.class)
                                                            .renderDocumentTemplateSearch(null, null, null)), "Document library")))
        .andExpect(model().attribute("previewWithoutConditionsUrl", ReverseRouter.route(on(DocumentTemplatePdfController.class).renderTemplatePreviewPdfWithoutConditions(DOCUMENT_TEMPLATE_ID, null))))
        .andExpect(model().attributeDoesNotExist("previewWithConditionsUrl"));
  }

  @Test
  void renderTemplateOverview_whenHasConditionalSections_twoPreviewUrls() throws Exception {
    DocumentTemplateDto documentTemplateDto = DocumentTemplateDtoTestUtil
        .newBuilder()
        .withId(DOCUMENT_TEMPLATE_ID)
        .build();

    var conditionalSection = new DocumentTemplateSectionSummaryView(
        UUID.randomUUID(),
        "1",
        "Test title",
        "Test content",
        "CONDITION_MNEMONIC",
        false,
        List.of(),
        Map.of(),
        DocumentTemplateSectionUrlsTestUtil.newBuilder().build(),
        List.of()
    );

    var nonConditionalSection = new DocumentTemplateSectionSummaryView(
        UUID.randomUUID(),
        "1",
        "Test title",
        "Test content",
        null,
        false,
        List.of(),
        Map.of(),
        DocumentTemplateSectionUrlsTestUtil.newBuilder().build(),
        List.of()
    );

    var documentSummaryView = DocumentTemplateSectionsSummaryViewTestUtil.newBuilder()
        .withDocumentTemplateSectionSummaryViews(List.of(conditionalSection, nonConditionalSection))
        .build();

    when(lmsDocumentTemplateService.getDocumentTemplateSectionsSummaryView(any(), any())).thenReturn(documentSummaryView);
    when(documentTemplateService.getDocumentTemplateDtoOrThrow(DOCUMENT_TEMPLATE_ID)).thenReturn(documentTemplateDto);

    mockMvc.perform(
            get(ReverseRouter.route(on(DocumentTemplateController.class)
                                        .renderTemplateOverview(DOCUMENT_TEMPLATE_ID, null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("previewWithoutConditionsUrl", ReverseRouter.route(on(DocumentTemplatePdfController.class).renderTemplatePreviewPdfWithoutConditions(DOCUMENT_TEMPLATE_ID, null))))
        .andExpect(model().attribute("previewWithConditionsUrl", ReverseRouter.route(on(DocumentTemplatePdfController.class).renderTemplatePreviewPdfWithConditions(DOCUMENT_TEMPLATE_ID, null))));
  }

  @Test
  void renderTemplateOverview_whenAllSectionsAreConditional() throws Exception {
    DocumentTemplateDto documentTemplateDto = DocumentTemplateDtoTestUtil
        .newBuilder()
        .withId(DOCUMENT_TEMPLATE_ID)
        .build();

    var nonConditionalSection = new DocumentTemplateSectionSummaryView(
        UUID.randomUUID(),
        "1",
        "Test title",
        "Test content",
        "CONDITION_MNEMONIC",
        false,
        List.of(),
        Map.of(),
        DocumentTemplateSectionUrlsTestUtil.newBuilder().build(),
        List.of()
    );

    var documentSummaryView = DocumentTemplateSectionsSummaryViewTestUtil.newBuilder()
        .withDocumentTemplateSectionSummaryViews(List.of(nonConditionalSection))
        .build();

    when(lmsDocumentTemplateService.getDocumentTemplateSectionsSummaryView(any(), any())).thenReturn(documentSummaryView);
    when(documentTemplateService.getDocumentTemplateDtoOrThrow(DOCUMENT_TEMPLATE_ID)).thenReturn(documentTemplateDto);

    mockMvc.perform(
            get(ReverseRouter.route(on(DocumentTemplateController.class)
                                        .renderTemplateOverview(DOCUMENT_TEMPLATE_ID, null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/document/templateOverview"))
        .andExpect(model().attribute("previewWithConditionsUrl", ReverseRouter.route(on(DocumentTemplatePdfController.class).renderTemplatePreviewPdfWithConditions(DOCUMENT_TEMPLATE_ID, null))))
        .andExpect(model().attributeDoesNotExist("previewWithoutConditionsUrl"));
  }
}
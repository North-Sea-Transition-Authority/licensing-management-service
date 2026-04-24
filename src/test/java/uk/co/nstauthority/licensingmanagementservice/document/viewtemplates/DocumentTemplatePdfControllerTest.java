package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionSummaryView;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateSectionUrlsTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = DocumentTemplatePdfController.class)
class DocumentTemplatePdfControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LmsDocumentTemplateService lmsDocumentTemplateService;

  @Test
  void renderTemplatePreviewPdf_WithoutConditions_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(DocumentTemplatePdfController.class)
                                                .renderTemplatePreviewPdfWithoutConditions(UUID.randomUUID(), null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderTemplatePreviewPdf_WithoutConditions_assertOk() throws Exception {
    var documentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().build();

    var pdfBytes = new byte[] {1, 2, 3};
    var pdfRenderResultWithGenerationData = new LmsPdfRenderResult(new ByteArrayResource(pdfBytes), "<html/>", Map.of());

    var nonConditionalSummaryView = List.of(
        new DocumentTemplateSectionSummaryView(
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
        )
    );
    when(lmsDocumentTemplateService.getAllNonConditionalTopLevelDocumentTemplateSectionSummaryViews(documentTemplateDto))
        .thenReturn(nonConditionalSummaryView);

    when(documentTemplateService.getDocumentTemplateDtoOrThrow(documentTemplateDto.id())).thenReturn(documentTemplateDto);
    when(lmsDocumentTemplateService.renderPdf(true, documentTemplateDto, nonConditionalSummaryView)).thenReturn(pdfRenderResultWithGenerationData);

    var fileName = "PREVIEW %s.pdf".formatted(documentTemplateDto.title());

    mockMvc.perform(get(ReverseRouter.route(on(DocumentTemplatePdfController.class)
                                                .renderTemplatePreviewPdfWithoutConditions(documentTemplateDto.id(), null)))
                        .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(content().bytes(pdfBytes))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(fileName)));
  }

  @Test
  void renderTemplatePreviewPdfWithConditions_whenNotLoggedIn_thenRedirectToLoginPage() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(DocumentTemplatePdfController.class)
                                                .renderTemplatePreviewPdfWithConditions(UUID.randomUUID(), null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderTemplatePreviewPdfWithConditions_assertOk() throws Exception {
    var documentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().build();

    var pdfBytes = new byte[] {1, 2, 3};
    var pdfRenderResultWithGenerationData = new LmsPdfRenderResult(new ByteArrayResource(pdfBytes), "<html/>", Map.of());

    var sectionSummaryView = List.of(
        new DocumentTemplateSectionSummaryView(
            UUID.randomUUID(),
            "1",
            "Test title",
            "Test content",
            "CONDITION",
            false,
            List.of(),
            Map.of(),
            DocumentTemplateSectionUrlsTestUtil
                .newBuilder().build(),
            List.of()
        )
    );

    var expectedSummaryViews = DocumentTemplateSectionsSummaryViewTestUtil.newBuilder()
        .withDocumentTemplateSectionSummaryViews(sectionSummaryView)
        .build();

    when(lmsDocumentTemplateService.getDocumentTemplateSectionsSummaryView(documentTemplateDto))
        .thenReturn(expectedSummaryViews);

    when(documentTemplateService.getDocumentTemplateDtoOrThrow(documentTemplateDto.id())).thenReturn(documentTemplateDto);
    when(lmsDocumentTemplateService.renderPdf(true, documentTemplateDto, sectionSummaryView)).thenReturn(pdfRenderResultWithGenerationData);

    var fileName = "PREVIEW %s.pdf".formatted(documentTemplateDto.title());

    mockMvc.perform(get(ReverseRouter.route(on(DocumentTemplatePdfController.class)
                                             .renderTemplatePreviewPdfWithConditions(documentTemplateDto.id(), null)))
                     .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(content().bytes(pdfBytes))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(fileName)));
  }
}
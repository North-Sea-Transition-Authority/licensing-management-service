package uk.co.nstauthority.licensingmanagementservice.document;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption.ADD_AFTER;
import static uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption.ADD_BEFORE;
import static uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption.ADD_SUBSECTION;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldFormatter;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionUrls;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionViewService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionsSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;
import uk.co.fivium.digitaldocumentlibrary.document.PdfRenderResult;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentTemplateSectionController;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentTemplateSectionDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentTemplateSectionsSummaryViewTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CompanyNameMailMergeField;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CompanyRegisteredAddressMailMergeField;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CurrentDateMailMergeField;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ExtendWith(MockitoExtension.class)
class LmsDocumentTemplateServiceTest {

  @Mock
  private DocumentTemplateSectionViewService documentTemplateSectionViewService;

  @InjectMocks
  private LmsDocumentTemplateService lmsDocumentTemplateService;

  @Mock
  private DocumentTemplateDto templateDto;

  @Mock
  DocumentTemplateSectionsSummaryView documentTemplateSectionsSummaryView;

  @Mock
  private DocumentMailMergeFieldFormatter formatter;

  @Mock
  private CompanyRegisteredAddressMailMergeField companyRegisteredAddressMailMergeField;

  @Mock
  private CurrentDateMailMergeField currentDateMailMergeField;

  @Mock
  private CompanyNameMailMergeField companyNameMailMergeField;

  @Mock
  private DocumentInstanceMailMergeFieldFormatter documentMailMergeFieldFormatter;

  @Mock
  private DocumentTemplateService documentTemplateService;

  @Test
  void getDocumentTemplateSectionsSummaryView() {
    when(documentTemplateSectionViewService.getDocumentTemplateSectionsSummaryView(any(), any(), any()))
        .thenReturn(documentTemplateSectionsSummaryView);

    DocumentTemplateSectionsSummaryView result = lmsDocumentTemplateService.getDocumentTemplateSectionsSummaryView(templateDto, formatter);

    assertEquals(documentTemplateSectionsSummaryView, result);

    verify(documentTemplateSectionViewService).getDocumentTemplateSectionsSummaryView(eq(templateDto), any(), eq(formatter));
  }

  @Test
  void renderPdf_whenTemplateModelValuesAreNullOrEmpty() {
    var documentTemplate = DocumentTemplateDtoTestUtil.newBuilder().build();
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

    when(companyNameMailMergeField.resolve(documentTemplate))
        .thenReturn(DocumentMailMergeFieldResolveResult.error("error"));
    when(companyRegisteredAddressMailMergeField.resolve(documentTemplate))
        .thenReturn(DocumentMailMergeFieldResolveResult.error("error"));
    when(currentDateMailMergeField.resolve(documentTemplate))
        .thenReturn(DocumentMailMergeFieldResolveResult.error("error"));

    var pdfRenderResult = new PdfRenderResult(new ByteArrayResource(new byte[] {1, 2, 3}), "<html/>");

    Map<String, Object> templateModel = Map.of(
        "documentTemplateSectionSummaryView",
        nonConditionalSummaryView,
        "isPreview",
        true,
        "companyName",
        "",
        "companyRegisteredAddress",
        List.of(),
        "currentDate", ""
    );

    when(documentTemplateService.renderPdf(eq(documentTemplate), any()))
        .thenReturn(pdfRenderResult);

    lmsDocumentTemplateService.renderPdf(true, documentTemplate, nonConditionalSummaryView);

    ArgumentCaptor<Map<String, Object>> argumentCaptor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService).renderPdf(eq(documentTemplate), argumentCaptor.capture());

    assertThat(argumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(templateModel);
  }

  @Test
  void getAllNonConditionalTopLevelDocumentTemplateSectionSummaryViews() {
    var nonConditionalSection = new DocumentTemplateSectionSummaryView(
        UUID.randomUUID(),
        "1", "Test title",
        "Test content",
        null, false,
        List.of(),
        Map.of(),
        DocumentTemplateSectionUrlsTestUtil.newBuilder().build(),
        List.of()
    );

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

    var expectedSummarySection = DocumentTemplateSectionsSummaryViewTestUtil.newBuilder()
        .withDocumentTemplateSectionSummaryViews(List.of(conditionalSection, nonConditionalSection))
        .build();

    when(documentTemplateSectionViewService.getDocumentTemplateSectionsSummaryView(eq(templateDto), any(), eq(documentMailMergeFieldFormatter)))
        .thenReturn(expectedSummarySection);

    var resultingNonConditionalSection =
        lmsDocumentTemplateService.getAllNonConditionalTopLevelDocumentTemplateSectionSummaryViews(templateDto);

    assertThat(resultingNonConditionalSection).containsExactly(nonConditionalSection);
  }

  @Test
  void getDocumentTemplateSectionsSummaryView_with_defaultMailMergeFormatter_verifyCalls() {
    var expectedSummarySection = DocumentTemplateSectionsSummaryViewTestUtil.newBuilder().build();

    when(documentTemplateSectionViewService.getDocumentTemplateSectionsSummaryView(eq(templateDto), any(), eq(documentMailMergeFieldFormatter)))
        .thenReturn(expectedSummarySection);

    var resultingSectionSummary = lmsDocumentTemplateService.getDocumentTemplateSectionsSummaryView(templateDto);

    assertEquals(expectedSummarySection, resultingSectionSummary);
  }

  @Test
  void getDocumentSectionUrls() {
    var sectionDto = DocumentTemplateSectionDtoTestUtil.newBuilder().build();

    var resultingUrls = lmsDocumentTemplateService.getDocumentSectionUrls(sectionDto);

    Assertions.assertThat(resultingUrls)
        .extracting(
            DocumentTemplateSectionUrls::addSectionBeforeUrl,
            DocumentTemplateSectionUrls::addSectionAfterUrl,
            DocumentTemplateSectionUrls::addSubsectionUrl,
            DocumentTemplateSectionUrls::editUrl,
            DocumentTemplateSectionUrls::removeUrl
        )
        .containsExactly(
            ReverseRouter.route(on(DocumentTemplateSectionController.class).renderAddSectionPage(sectionDto.id(), ADD_BEFORE)),
            ReverseRouter.route(on(DocumentTemplateSectionController.class).renderAddSectionPage(sectionDto.id(), ADD_AFTER)),
            ReverseRouter.route(on(DocumentTemplateSectionController.class).renderAddSectionPage(sectionDto.id(), ADD_SUBSECTION)),
            ReverseRouter.route(on(DocumentTemplateSectionController.class).renderEditSectionPage(sectionDto.id())),
            ReverseRouter.route(on(DocumentTemplateSectionController.class).renderRemoveSectionPage(sectionDto.id()))
        );
  }
}
package uk.co.nstauthority.licensingmanagementservice.document.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionUrls;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionViewService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldFormatter;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.digitaldocumentlibrary.document.PdfRenderResult;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentInstanceMailMergeFieldFormatter;
import uk.co.nstauthority.licensingmanagementservice.document.signing.DocumentSigningService;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.LmsPdfRenderResult;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CompanyNameMailMergeField;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CompanyRegisteredAddressMailMergeField;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CurrentDateMailMergeField;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterDocumentController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ExtendWith(MockitoExtension.class)
class LmsDocumentInstanceServiceTest {

  @Mock
  private DocumentInstanceMailMergeFieldFormatter documentMailMergeFieldFormatter;

  @Mock
  private DocumentInstanceSectionViewService documentInstanceSectionViewService;

  @Mock
  private LicenceApplication application;

  @Mock
  private DocumentInstanceService documentInstanceService;

  @Mock
  private CompanyNameMailMergeField companyNameMailMergeField;

  @Mock
  private CompanyRegisteredAddressMailMergeField companyRegisteredAddressMailMergeField;

  @Mock
  private CurrentDateMailMergeField currentDateMailMergeField;

  @Mock
  private DocumentSigningService documentSigningService;

  private ServiceUserDetail serviceUserDetail;

  @Captor
  private ArgumentCaptor<Map<String, Object>> templateModelCaptor;

  @InjectMocks
  private LmsDocumentInstanceService lmsDocumentInstanceService;

  @BeforeEach
  void setUp() {
    serviceUserDetail = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(1L)
        .build();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void getDocumentInstanceSectionsSummaryView(boolean useDocumentMailMergeFieldFormatter) {
    var sectionSummaryView = new DocumentInstanceSectionSummaryView(
        UUID.randomUUID(), "1", "Test title", "Test content", false,
        List.of(), Map.of(), DocumentInstanceSectionUrlsTestUtil.newBuilder().build(), List.of()
    );

    var sectionsSummaryView = DocumentInstanceSectionsSummaryView.from(List.of(sectionSummaryView));
    var formatter = useDocumentMailMergeFieldFormatter ? documentMailMergeFieldFormatter : DocumentMailMergeFieldFormatter.noOp();
    var documentInstance = DocumentInstanceDtoTestUtil.newBuilder().build();

    when(documentInstanceSectionViewService.getDocumentInstanceSectionsSummaryView(eq(documentInstance), any(), eq(formatter)))
        .thenReturn(sectionsSummaryView);

    var result = lmsDocumentInstanceService.getDocumentInstanceSectionsSummaryView(
        documentInstance,
        useDocumentMailMergeFieldFormatter,
        application
    );

    assertThat(result)
        .extracting(DocumentInstanceSectionsSummaryView::topLevelDocumentInstanceSectionSummaryViews)
        .isEqualTo(List.of(sectionSummaryView));
  }

  @Test
  void getDocumentInstanceSectionUrls() {
    var appId = UUID.randomUUID();
    var appType = ApplicationType.CONTINUATION_APPLICATION;
    when(application.getId()).thenReturn(appId);
    when(application.getApplicationType()).thenReturn(appType);

    var sectionDto = DocumentInstanceSectionDtoTestUtil.newBuilder().build();
    var resultingUrls = LmsDocumentInstanceService.getDocumentInstanceSectionUrls(application, sectionDto);
    var expectedController = ApplicationLetterDocumentController.class;

    assertThat(resultingUrls)
        .extracting(
            DocumentInstanceSectionUrls::addSectionBeforeUrl,
            DocumentInstanceSectionUrls::addSectionAfterUrl,
            DocumentInstanceSectionUrls::addSubsectionUrl,
            DocumentInstanceSectionUrls::editUrl,
            DocumentInstanceSectionUrls::removeUrl
        )
        .containsExactly(
            ReverseRouter.route(on(expectedController).renderAddSectionPage(appType, appId, sectionDto.id(), AddSectionOption.ADD_BEFORE)),
            ReverseRouter.route(on(expectedController).renderAddSectionPage(appType, appId, sectionDto.id(), AddSectionOption.ADD_AFTER)),
            ReverseRouter.route(on(expectedController).renderAddSectionPage(appType, appId, sectionDto.id(), AddSectionOption.ADD_SUBSECTION)),
            ReverseRouter.route(on(expectedController).renderEditSectionPage(appType, appId, sectionDto.id())),
            ReverseRouter.route(on(expectedController).renderRemoveSectionPage(appType, appId, sectionDto.id()))
        );
  }

  @Test
  void getDocumentInstanceSectionErrors() {
    var sectionDto = DocumentInstanceSectionDtoTestUtil.newBuilder().build();
    List<String> expectedErrors = List.of("Error 1", "Error 2");

    when(documentInstanceSectionViewService.getDocumentInstanceSectionErrorMessages(eq(sectionDto), any()))
        .thenReturn(expectedErrors);

    var result = lmsDocumentInstanceService.getDocumentInstanceSectionErrors(sectionDto);
    assertThat(result).isEqualTo(expectedErrors);
  }

  @Test
  void renderAndSignPdf_whenFieldsResolveSuccessfully_returnsPdfRenderResult() {
    var isPreview = true;
    var documentInstance = DocumentInstanceDtoTestUtil.newBuilder().build();
    var summaryViews = List.of(new DocumentInstanceSectionSummaryView(
        UUID.randomUUID(),
        "1",
        "Test title",
        "Test content",
        false,
        List.of(),
        Map.of(),
        DocumentInstanceSectionUrlsTestUtil.newBuilder().build(),
        List.of()
    ));

    var expectedPdfContent = new ByteArrayResource(new byte[]{1, 2, 3});
    var expectedHtml = "<p>Test PDF HTML</p>";
    var pdfRenderResult = new PdfRenderResult(expectedPdfContent, expectedHtml);

    when(companyNameMailMergeField.resolve(documentInstance))
        .thenReturn(DocumentMailMergeFieldResolveResult.success("Test Company"));
    when(companyRegisteredAddressMailMergeField.resolve(documentInstance))
        .thenReturn(DocumentMailMergeFieldResolveResult.success("123 Test Street\nTest City"));
    when(currentDateMailMergeField.resolve(documentInstance))
        .thenReturn(DocumentMailMergeFieldResolveResult.success("16 March 2026"));
    when(documentSigningService.previewPdfSignature(pdfRenderResult.pdfContent())).thenReturn(expectedPdfContent);

    when(documentInstanceService.renderPdf(eq(documentInstance), any(Map.class)))
        .thenReturn(pdfRenderResult);

    Map<String, Object> expectedTemplateModel = Map.of(
        "documentInstanceSectionSummaryView", summaryViews,
        "isPreview", isPreview,
        "companyName", "Test Company",
        "companyRegisteredAddress", new String[]{"123 Test Street", "Test City"},
        "currentDate", "16 March 2026"
    );

    var result = lmsDocumentInstanceService.renderAndSignPdf(application, isPreview, documentInstance, summaryViews, null);

    verify(documentInstanceService).renderPdf(eq(documentInstance), templateModelCaptor.capture());
    assertThat(templateModelCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedTemplateModel);

    var documentInstanceSectionsSummaryView = DocumentInstanceSectionsSummaryView.from(summaryViews);

    assertThat(result)
        .extracting(
            LmsPdfRenderResult::pdfContent,
            LmsPdfRenderResult::pdfHtml,
            LmsPdfRenderResult::mailMergeResolvedValuesByMnemonic
        )
        .containsExactly(
            expectedPdfContent,
            expectedHtml,
            documentInstanceSectionsSummaryView.allMailMergeResolvedValuesByMnemonic()
        );
  }

  @Test
  void renderAndSignPdf_whenFieldsAreNullOrEmpty_handlesGracefully() {
    var isPreview = true;
    var documentInstance = DocumentInstanceDtoTestUtil.newBuilder().build();
    var summaryViews = List.of(new DocumentInstanceSectionSummaryView(
        UUID.randomUUID(),
        "1",
        "Test title",
        "Test content",
        false,
        List.of(),
        Map.of(),
        DocumentInstanceSectionUrlsTestUtil.newBuilder().build(),
        List.of()
    ));

    var expectedPdfContent = new ByteArrayResource(new byte[]{4, 5, 6});
    var expectedHtml = "<p>Empty Fields HTML</p>";
    var pdfRenderResult = new PdfRenderResult(expectedPdfContent, expectedHtml);

    when(companyNameMailMergeField.resolve(documentInstance))
        .thenReturn(DocumentMailMergeFieldResolveResult.error("Failed to resolve"));
    when(companyRegisteredAddressMailMergeField.resolve(documentInstance))
        .thenReturn(DocumentMailMergeFieldResolveResult.error("Failed to resolve"));
    when(currentDateMailMergeField.resolve(documentInstance))
        .thenReturn(DocumentMailMergeFieldResolveResult.success(null));

    when(documentSigningService.previewPdfSignature(pdfRenderResult.pdfContent())).thenReturn(new ByteArrayResource(new byte[]{4, 5, 6}));

    when(documentInstanceService.renderPdf(eq(documentInstance), any(Map.class)))
        .thenReturn(pdfRenderResult);

    Map<String, Object> expectedTemplateModel = Map.of(
        "documentInstanceSectionSummaryView", summaryViews,
        "isPreview", isPreview,
        "companyName", "",
        "companyRegisteredAddress", List.of(),
        "currentDate", ""
    );

    var result = lmsDocumentInstanceService.renderAndSignPdf(application, isPreview, documentInstance, summaryViews, null);

    verify(documentInstanceService).renderPdf(eq(documentInstance), templateModelCaptor.capture());
    assertThat(templateModelCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedTemplateModel);

    var documentInstanceSectionsSummaryView = DocumentInstanceSectionsSummaryView.from(summaryViews);

    assertThat(result)
        .extracting(
            LmsPdfRenderResult::pdfContent,
            LmsPdfRenderResult::pdfHtml,
            LmsPdfRenderResult::mailMergeResolvedValuesByMnemonic
        )
        .containsExactly(
            expectedPdfContent,
            expectedHtml,
            documentInstanceSectionsSummaryView.allMailMergeResolvedValuesByMnemonic()
        );
  }

  @Test
  void renderAndSignPdf_whenFieldsResolveSuccessfully_returnsPdfRenderResult_IsNotPreview() {
    var isPreview = false;
    var documentInstance = DocumentInstanceDtoTestUtil.newBuilder().build();
    var summaryViews = List.of(new DocumentInstanceSectionSummaryView(
        UUID.randomUUID(),
        "1",
        "Test title",
        "Test content",
        false,
        List.of(),
        Map.of(),
        DocumentInstanceSectionUrlsTestUtil.newBuilder().build(),
        List.of()
    ));

    var expectedPdfContent = new ByteArrayResource(new byte[]{1, 2, 3});
    var expectedHtml = "<p>Test PDF HTML</p>";
    var pdfRenderResult = new PdfRenderResult(expectedPdfContent, expectedHtml);

    when(companyNameMailMergeField.resolve(documentInstance))
        .thenReturn(DocumentMailMergeFieldResolveResult.success("Test Company"));
    when(companyRegisteredAddressMailMergeField.resolve(documentInstance))
        .thenReturn(DocumentMailMergeFieldResolveResult.success("123 Test Street\nTest City"));
    when(currentDateMailMergeField.resolve(documentInstance))
        .thenReturn(DocumentMailMergeFieldResolveResult.success("16 March 2026"));

    when(application.getApplicationType()).thenReturn(ApplicationType.CONTINUATION_APPLICATION);

    when(documentInstanceService.renderPdf(eq(documentInstance), any(Map.class)))
        .thenReturn(pdfRenderResult);

    Map<String, Object> expectedTemplateModel = Map.of(
        "documentInstanceSectionSummaryView", summaryViews,
        "isPreview", isPreview,
        "companyName", "Test Company",
        "companyRegisteredAddress", new String[]{"123 Test Street", "Test City"},
        "currentDate", "16 March 2026"
    );
    when(documentSigningService.signPdf(pdfRenderResult.pdfContent(), serviceUserDetail)).thenReturn(expectedPdfContent);

    var result = lmsDocumentInstanceService.renderAndSignPdf(application, isPreview, documentInstance, summaryViews, serviceUserDetail);

    verify(documentInstanceService).renderPdf(eq(documentInstance), templateModelCaptor.capture());
    assertThat(templateModelCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedTemplateModel);

    var documentInstanceSectionsSummaryView = DocumentInstanceSectionsSummaryView.from(summaryViews);

    assertThat(result)
        .extracting(
            LmsPdfRenderResult::pdfContent,
            LmsPdfRenderResult::pdfHtml,
            LmsPdfRenderResult::mailMergeResolvedValuesByMnemonic
        )
        .containsExactly(
            expectedPdfContent,
            expectedHtml,
            documentInstanceSectionsSummaryView.allMailMergeResolvedValuesByMnemonic()
        );
  }

  @Test
  void renderAndSignPdf_whenFieldsAreNullOrEmpty_handlesGracefully_IsNotPreview() {
    var isPreview = false;
    var documentInstance = DocumentInstanceDtoTestUtil.newBuilder().build();
    var summaryViews = List.of(new DocumentInstanceSectionSummaryView(
        UUID.randomUUID(),
        "1",
        "Test title",
        "Test content",
        false,
        List.of(),
        Map.of(),
        DocumentInstanceSectionUrlsTestUtil.newBuilder().build(),
        List.of()
    ));

    var expectedPdfContent = new ByteArrayResource(new byte[]{4, 5, 6});
    var expectedHtml = "<p>Empty Fields HTML</p>";
    var pdfRenderResult = new PdfRenderResult(expectedPdfContent, expectedHtml);

    when(companyNameMailMergeField.resolve(documentInstance))
        .thenReturn(DocumentMailMergeFieldResolveResult.error("Failed to resolve"));
    when(companyRegisteredAddressMailMergeField.resolve(documentInstance))
        .thenReturn(DocumentMailMergeFieldResolveResult.error("Failed to resolve"));
    when(currentDateMailMergeField.resolve(documentInstance))
        .thenReturn(DocumentMailMergeFieldResolveResult.success(null));

    when(documentInstanceService.renderPdf(eq(documentInstance), any(Map.class)))
        .thenReturn(pdfRenderResult);
    when(documentSigningService.signPdf(pdfRenderResult.pdfContent(), serviceUserDetail)).thenReturn(new ByteArrayResource(new byte[]{4, 5, 6}));

    when(application.getApplicationType()).thenReturn(ApplicationType.CONTINUATION_APPLICATION);

    Map<String, Object> expectedTemplateModel = Map.of(
        "documentInstanceSectionSummaryView", summaryViews,
        "isPreview", isPreview,
        "companyName", "",
        "companyRegisteredAddress", List.of(),
        "currentDate", ""
    );

    var result = lmsDocumentInstanceService.renderAndSignPdf(application, isPreview, documentInstance, summaryViews, serviceUserDetail);

    verify(documentInstanceService).renderPdf(eq(documentInstance), templateModelCaptor.capture());
    assertThat(templateModelCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedTemplateModel);

    var documentInstanceSectionsSummaryView = DocumentInstanceSectionsSummaryView.from(summaryViews);

    assertThat(result)
        .extracting(
            LmsPdfRenderResult::pdfContent,
            LmsPdfRenderResult::pdfHtml,
            LmsPdfRenderResult::mailMergeResolvedValuesByMnemonic
        )
        .containsExactly(
            expectedPdfContent,
            expectedHtml,
            documentInstanceSectionsSummaryView.allMailMergeResolvedValuesByMnemonic()
        );
  }
}
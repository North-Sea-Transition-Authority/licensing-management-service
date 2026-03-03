package uk.co.nstauthority.licensingmanagementservice.document.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionUrls;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionViewService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldFormatter;
import uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentInstanceMailMergeFieldFormatter;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;
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
  LicenceApplication application;

  @InjectMocks
  private LmsDocumentInstanceService lmsDocumentInstanceService;

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
}
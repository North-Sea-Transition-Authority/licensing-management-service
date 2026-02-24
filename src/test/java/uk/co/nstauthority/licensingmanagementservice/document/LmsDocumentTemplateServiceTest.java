package uk.co.nstauthority.licensingmanagementservice.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldFormatter;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionViewService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionsSummaryView;

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

  @Test
  void getDocumentTemplateSectionsSummaryView() {
    when(documentTemplateSectionViewService.getDocumentTemplateSectionsSummaryView(any(), any(), any()))
        .thenReturn(documentTemplateSectionsSummaryView);

    DocumentTemplateSectionsSummaryView result = lmsDocumentTemplateService.getDocumentTemplateSectionsSummaryView(templateDto, formatter);

    assertEquals(documentTemplateSectionsSummaryView, result);

    verify(documentTemplateSectionViewService).getDocumentTemplateSectionsSummaryView(eq(templateDto), any(), eq(formatter));
  }
}
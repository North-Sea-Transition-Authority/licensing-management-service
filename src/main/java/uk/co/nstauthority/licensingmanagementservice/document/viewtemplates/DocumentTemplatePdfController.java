package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates;

import com.google.common.net.HttpHeaders;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionSummaryView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.HasAnyRoleInTeamTypeInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("document-library/{documentTemplateId}/preview-template")
@HasAnyRoleInTeamTypeInterceptorRule.HasAnyRoleInTeamType(TeamType.LICENCE_MANAGEMENT)
public class DocumentTemplatePdfController {

  private final LmsDocumentTemplateService lmsDocumentTemplateService;

  @Autowired
  public DocumentTemplatePdfController(LmsDocumentTemplateService lmsDocumentTemplateService) {
    this.lmsDocumentTemplateService = lmsDocumentTemplateService;
  }

  @GetMapping("without-conditions")
  public ResponseEntity<ByteArrayResource> renderTemplatePreviewPdfWithoutConditions(
      @PathVariable UUID documentTemplateId,
      DocumentTemplateDto documentTemplateDto
  ) {
    var nonConditionalSummaryView = lmsDocumentTemplateService
        .getAllNonConditionalTopLevelDocumentTemplateSectionSummaryViews(documentTemplateDto);

    return renderPdf(documentTemplateDto, nonConditionalSummaryView);
  }

  @GetMapping("/with-conditions")
  public ResponseEntity<ByteArrayResource> renderTemplatePreviewPdfWithConditions(
      @PathVariable UUID documentTemplateId,
      DocumentTemplateDto documentTemplateDto
  ) {
    var sectionsSummaryView = lmsDocumentTemplateService.getDocumentTemplateSectionsSummaryView(documentTemplateDto);
    return renderPdf(documentTemplateDto, sectionsSummaryView.topLevelDocumentTemplateSectionSummaryViews());
  }

  private ResponseEntity<ByteArrayResource> renderPdf(
      DocumentTemplateDto documentTemplateDto,
      List<DocumentTemplateSectionSummaryView> sectionsSummaryView
  ) {
    var pdf = lmsDocumentTemplateService.renderPdf(
        true,
        documentTemplateDto,
        sectionsSummaryView
    );

    ByteArrayResource pdfContent = pdf.pdfContent();

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(pdfContent.contentLength())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"%s\"".formatted("PREVIEW %s.pdf".formatted(documentTemplateDto.title()))
        )
        .body(pdfContent);
  }
}
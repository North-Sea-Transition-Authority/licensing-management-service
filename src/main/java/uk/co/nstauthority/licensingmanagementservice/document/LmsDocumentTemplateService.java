package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldFormatter;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionUrls;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionViewService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionsSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.LmsPdfRenderResult;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CompanyNameMailMergeField;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CompanyRegisteredAddressMailMergeField;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CurrentDateMailMergeField;

@Service
public class LmsDocumentTemplateService {

  private final DocumentTemplateSectionViewService documentTemplateSectionViewService;
  private final DocumentInstanceMailMergeFieldFormatter documentInstanceMailMergeFieldFormatter;
  private final CompanyNameMailMergeField companyNameMailMergeField;
  private final CurrentDateMailMergeField currentDateMailMergeField;
  private final CompanyRegisteredAddressMailMergeField companyRegisteredAddressMailMergeField;
  private final DocumentTemplateService documentTemplateService;

  @Autowired
  public LmsDocumentTemplateService(
      DocumentTemplateSectionViewService documentTemplateSectionViewService,
      DocumentInstanceMailMergeFieldFormatter documentInstanceMailMergeFieldFormatter,
      CompanyNameMailMergeField companyNameMailMergeField,
      CurrentDateMailMergeField currentDateMailMergeField,
      CompanyRegisteredAddressMailMergeField companyRegisteredAddressMailMergeField,
      DocumentTemplateService documentTemplateService
  ) {
    this.documentTemplateSectionViewService = documentTemplateSectionViewService;
    this.documentInstanceMailMergeFieldFormatter = documentInstanceMailMergeFieldFormatter;
    this.companyNameMailMergeField = companyNameMailMergeField;
    this.currentDateMailMergeField = currentDateMailMergeField;
    this.companyRegisteredAddressMailMergeField = companyRegisteredAddressMailMergeField;
    this.documentTemplateService = documentTemplateService;
  }

  public DocumentTemplateSectionsSummaryView getDocumentTemplateSectionsSummaryView(
      DocumentTemplateDto documentTemplateDto,
      DocumentMailMergeFieldFormatter mailMergeFormatter
  ) {
    return documentTemplateSectionViewService.getDocumentTemplateSectionsSummaryView(
        documentTemplateDto,
        this::getDocumentSectionUrls,
        mailMergeFormatter
    );
  }

  public DocumentTemplateSectionsSummaryView getDocumentTemplateSectionsSummaryView(DocumentTemplateDto documentTemplateDto) {
    return getDocumentTemplateSectionsSummaryView(
        documentTemplateDto,
        documentInstanceMailMergeFieldFormatter
    );
  }

  public List<DocumentTemplateSectionSummaryView> getAllNonConditionalTopLevelDocumentTemplateSectionSummaryViews(
      DocumentTemplateDto documentTemplateDto
  ) {
    return getDocumentTemplateSectionsSummaryView(documentTemplateDto, documentInstanceMailMergeFieldFormatter)
        .topLevelDocumentTemplateSectionSummaryViews()
        .stream()
        .filter(sectionSummaryView -> StringUtils.isBlank(sectionSummaryView.conditionTitle()))
        .toList();
  }

  public LmsPdfRenderResult renderPdf(
      boolean isPreview,
      DocumentTemplateDto documentTemplateDto,
      List<DocumentTemplateSectionSummaryView> nonConditionalSummaryView
  ) {
    var companyAddress = companyRegisteredAddressMailMergeField.resolve(documentTemplateDto).resolvedValue();
    var companyName = companyNameMailMergeField.resolve(documentTemplateDto);
    var currentDate = currentDateMailMergeField.resolve(documentTemplateDto);

    Map<String, Object> templateModel = new HashMap<>(Map.of(
        "documentTemplateSectionSummaryView", nonConditionalSummaryView,
        "isPreview", isPreview,
        "companyName", getResolvedValue(companyName),
        "companyRegisteredAddress", (companyAddress != null) ? companyAddress.split("\n") : List.of(),
        "currentDate", getResolvedValue(currentDate)
    ));

    var pdf = documentTemplateService.renderPdf(documentTemplateDto, templateModel);
    return new LmsPdfRenderResult(pdf.pdfContent(), pdf.pdfHtml(), Map.of());
  }

  //TODO add real world section urls
  DocumentTemplateSectionUrls getDocumentSectionUrls(DocumentTemplateSectionDto documentTemplateSectionDto) {
    return new DocumentTemplateSectionUrls("", "", "", "", "");
  }

  private String getResolvedValue(DocumentMailMergeFieldResolveResult resolveResult) {
    return (resolveResult != null) && ((resolveResult.resolvedValue()) != null) ? resolveResult.resolvedValue() : "";
  }
}
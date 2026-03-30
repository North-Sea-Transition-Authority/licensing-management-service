package uk.co.nstauthority.licensingmanagementservice.document.instance;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionUrls;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionViewService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldFormatter;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.digitaldocumentlibrary.document.PdfRenderResult;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentInstanceMailMergeFieldFormatter;
import uk.co.nstauthority.licensingmanagementservice.document.signing.DocumentSigningService;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.LmsPdfRenderResult;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CompanyNameMailMergeField;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CompanyRegisteredAddressMailMergeField;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CurrentDateMailMergeField;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterDocumentController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Service
public class LmsDocumentInstanceService {

  private final DocumentInstanceMailMergeFieldFormatter documentMailMergeFieldFormatter;
  private final DocumentInstanceSectionViewService documentInstanceSectionViewService;

  private final CompanyNameMailMergeField companyNameMailMergeField;
  private final CurrentDateMailMergeField currentDateMailMergeField;
  private final CompanyRegisteredAddressMailMergeField companyRegisteredAddressMailMergeField;
  private final DocumentInstanceService documentInstanceService;
  public final DocumentSigningService documentSigningService;
  private static final Logger LOGGER = LoggerFactory.getLogger(LmsDocumentInstanceService.class);


  @Autowired
  public LmsDocumentInstanceService(
      DocumentInstanceMailMergeFieldFormatter documentMailMergeFieldFormatter,
      DocumentInstanceSectionViewService documentInstanceSectionViewService,
      CompanyNameMailMergeField companyNameMailMergeField,
      CurrentDateMailMergeField currentDateMailMergeField,
      CompanyRegisteredAddressMailMergeField companyRegisteredAddressMailMergeField,
      DocumentInstanceService documentInstanceService,
      DocumentSigningService documentSigningService
  ) {
    this.documentMailMergeFieldFormatter = documentMailMergeFieldFormatter;
    this.documentInstanceSectionViewService = documentInstanceSectionViewService;
    this.companyNameMailMergeField = companyNameMailMergeField;
    this.currentDateMailMergeField = currentDateMailMergeField;
    this.companyRegisteredAddressMailMergeField = companyRegisteredAddressMailMergeField;
    this.documentInstanceService = documentInstanceService;
    this.documentSigningService = documentSigningService;
  }

  public DocumentInstanceSectionsSummaryView getDocumentInstanceSectionsSummaryView(
      DocumentInstanceDto documentInstanceDto,
      boolean useDocumentMailMergeFieldFormatter,
      LicenceApplication application
  ) {
    return documentInstanceSectionViewService.getDocumentInstanceSectionsSummaryView(
        documentInstanceDto,
        documentInstanceSectionDto -> getDocumentInstanceSectionUrls(application, documentInstanceSectionDto),
        useDocumentMailMergeFieldFormatter ? documentMailMergeFieldFormatter : DocumentMailMergeFieldFormatter.noOp()
    );
  }

  public List<String> getDocumentInstanceSectionErrors(DocumentInstanceSectionDto documentInstanceSectionDto) {
    return documentInstanceSectionViewService.getDocumentInstanceSectionErrorMessages(
        documentInstanceSectionDto,
        DocumentMailMergeFieldFormatter.noOp()
    );
  }

  static DocumentInstanceSectionUrls getDocumentInstanceSectionUrls(
      LicenceApplication application,
      DocumentInstanceSectionDto documentInstanceSectionDto
  ) {
    return new DocumentInstanceSectionUrls(
        ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderAddSectionPage(
            application.getApplicationType(),
            application.getId(),
            documentInstanceSectionDto.id(),
            AddSectionOption.ADD_BEFORE
        )),
        ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderAddSectionPage(
            application.getApplicationType(),
            application.getId(),
            documentInstanceSectionDto.id(),
            AddSectionOption.ADD_AFTER
        )),
        ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderAddSectionPage(
            application.getApplicationType(),
            application.getId(),
            documentInstanceSectionDto.id(),
            AddSectionOption.ADD_SUBSECTION
        )),
        ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderEditSectionPage(
            application.getApplicationType(),
            application.getId(),
            documentInstanceSectionDto.id()
        )),
        ReverseRouter.route(on(ApplicationLetterDocumentController.class).renderRemoveSectionPage(
            application.getApplicationType(),
            application.getId(),
            documentInstanceSectionDto.id()
        ))
    );
  }

  public LmsPdfRenderResult renderAndSignPdf(
      LicenceApplication application,
      boolean isPreview,
      DocumentInstanceDto documentInstanceDto,
      List<DocumentInstanceSectionSummaryView> documentInstanceSectionSummaryViews,
      ServiceUserDetail user
  ) {
    PdfRenderResult unsignedPdf = renderPdf(documentInstanceDto, documentInstanceSectionSummaryViews, isPreview);
    ByteArrayResource resultResource;

    if (isPreview) {
      resultResource = documentSigningService.previewPdfSignature(unsignedPdf.pdfContent());
    } else {
      resultResource = documentSigningService.signPdf(unsignedPdf.pdfContent(), user);

      LOGGER.info(
          "Digitally signed {} document for {} {} on behalf of user {}",
          documentInstanceDto.title(),
          application.getApplicationType().getDisplayName(),
          application.getId(),
          user.wuaId()
      );
    }

    var documentInstanceSectionsSummaryView = DocumentInstanceSectionsSummaryView.from(documentInstanceSectionSummaryViews);

    return new LmsPdfRenderResult(
        resultResource,
        unsignedPdf.pdfHtml(),
        documentInstanceSectionsSummaryView.allMailMergeResolvedValuesByMnemonic()
    );
  }

  private PdfRenderResult renderPdf(
      DocumentInstanceDto documentInstanceDto,
      List<DocumentInstanceSectionSummaryView> documentInstanceSectionSummaryViews,
      boolean isPreview
  ) {
    var companyAddress = companyRegisteredAddressMailMergeField.resolve(documentInstanceDto).resolvedValue();
    var companyName = companyNameMailMergeField.resolve(documentInstanceDto);
    var currentDate = currentDateMailMergeField.resolve(documentInstanceDto);

    var templateModel = getTemplateModel(
        documentInstanceSectionSummaryViews,
        isPreview,
        companyName,
        companyAddress,
        currentDate
    );

    return documentInstanceService.renderPdf(documentInstanceDto, templateModel);
  }

  private Map<String, Object> getTemplateModel(
      List<DocumentInstanceSectionSummaryView> documentInstanceSectionSummaryViews,
      boolean isPreview,
      DocumentMailMergeFieldResolveResult companyName,
      String companyAddress,
      DocumentMailMergeFieldResolveResult currentDate
  ) {
    return new HashMap<>(Map.of(
        "documentInstanceSectionSummaryView",
        documentInstanceSectionSummaryViews,
        "isPreview",
        isPreview,
        "companyName", getResolvedValue(companyName),
        "companyRegisteredAddress", (companyAddress != null) ? companyAddress.split("\n") : List.of(),
        "currentDate", getResolvedValue(currentDate)
    ));
  }

  private String getResolvedValue(DocumentMailMergeFieldResolveResult resolveResult) {
    return Optional.ofNullable(resolveResult)
        .map(DocumentMailMergeFieldResolveResult::resolvedValue)
        .orElse("");
  }
}
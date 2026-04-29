package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import com.google.common.net.HttpHeaders;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceNotFoundException;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentLinkingService;
import uk.co.nstauthority.licensingmanagementservice.document.instance.LmsDocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/application/{applicationType}/{applicationId}/document/{documentInstanceId}")
public class ApplicationDocumentActionsController {

  private final DocumentInstanceService documentInstanceService;
  private final LmsDocumentInstanceService lmsDocumentInstanceService;
  private final ApplicationService applicationService;
  private final DocumentLinkingService documentLinkingService;

  @Autowired
  public ApplicationDocumentActionsController(
      DocumentInstanceService documentInstanceService,
      LmsDocumentInstanceService lmsDocumentInstanceService,
      ApplicationService applicationService,
      DocumentLinkingService documentLinkingService
  ) {
    this.documentInstanceService = documentInstanceService;
    this.lmsDocumentInstanceService = lmsDocumentInstanceService;
    this.applicationService = applicationService;
    this.documentLinkingService = documentLinkingService;
  }

  @GetMapping("preview")
  public ModelAndView renderPreviewPdf(
      @PathVariable ApplicationType applicationType,
      @PathVariable UUID applicationId,
      @PathVariable("documentInstanceId") UUID documentInstanceId,
      ServiceUserDetail user
  ) {
    return ReverseRouter.redirect(on(ApplicationDocumentActionsController.class)
                                      .renderGeneratedPdf(applicationType, applicationId, documentInstanceId, user));
  }

  @GetMapping("render")
  public ResponseEntity<ByteArrayResource> renderGeneratedPdf(
      @PathVariable ApplicationType applicationType,
      @PathVariable UUID applicationId,
      @PathVariable("documentInstanceId") UUID documentInstanceId,
      ServiceUserDetail user
  ) {
    var application = applicationService.getApplication(applicationType, applicationId);
    var documentInstance = getDocumentInstanceDtoOrThrowNotFound(documentInstanceId);

    var sectionsSummaryView = lmsDocumentInstanceService
        .getDocumentInstanceSectionsSummaryView(documentInstance, false, application);

    return renderPreviewPdf(application, documentInstance, sectionsSummaryView, user);
  }

  @GetMapping("reload")
  public ModelAndView renderReloadDocumentPage(
      @PathVariable ApplicationType applicationType,
      @PathVariable UUID applicationId,
      @PathVariable("documentInstanceId") UUID documentInstanceId
  ) {
    var documentInstance = getDocumentInstanceDtoOrThrowNotFound(documentInstanceId);
    var organisationName = documentLinkingService.getApplicationCompanyNameFromDto(documentInstance);

    return new ModelAndView("lms/licence/application/letter/reloadDocumentInstance")
        .addObject("documentTitle", documentInstance.documentTemplateDto().title())
        .addObject("companyName", organisationName)
        .addObject(
            "cancelUrl",
            ReverseRouter.route(on(ApplicationLetterController.class)
                                    .renderEditLetterOverview(applicationType, applicationId))
        );
  }

  @PostMapping("reload")
  public ModelAndView reloadDocument(
      @PathVariable ApplicationType applicationType,
      @PathVariable UUID applicationId,
      @PathVariable("documentInstanceId") UUID documentInstanceId,
      RedirectAttributes redirectAttributes
  ) {
    var documentInstance = getDocumentInstanceDtoOrThrowNotFound(documentInstanceId);

    documentInstanceService.reloadDocumentInstance(documentInstance);

    NotificationBanner.newSuccessBannerWithHeader(
        "Successfully reloaded document %s".formatted(documentInstance.title()),
        redirectAttributes
    );

    return ReverseRouter.redirect(on(ApplicationLetterController.class)
                                      .renderEditLetterOverview(applicationType, applicationId));
  }

  private ResponseEntity<ByteArrayResource> renderPreviewPdf(
      LicenceApplication licenceApplication,
      DocumentInstanceDto documentInstance,
      DocumentInstanceSectionsSummaryView sectionsSummaryView,
      ServiceUserDetail user
  ) {
    var pdf = lmsDocumentInstanceService.renderAndSignPdf(
        licenceApplication,
        true,
        documentInstance,
        sectionsSummaryView.topLevelDocumentInstanceSectionSummaryViews(),
        user
    );

    ByteArrayResource pdfContent = pdf.pdfContent();
    var fileName = "PREVIEW %s.pdf".formatted(documentInstance.title());

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(pdfContent.contentLength())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"%s\"".formatted(fileName))
        .body(pdfContent);
  }

  private DocumentInstanceDto getDocumentInstanceDtoOrThrowNotFound(UUID documentInstanceId) {
    try {
      return documentInstanceService.getDocumentInstanceDtoOrThrow(documentInstanceId);
    } catch (DocumentInstanceNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }
}
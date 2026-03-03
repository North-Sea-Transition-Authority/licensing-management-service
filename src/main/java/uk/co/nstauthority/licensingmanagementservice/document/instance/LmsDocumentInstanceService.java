package uk.co.nstauthority.licensingmanagementservice.document.instance;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionUrls;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionViewService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldFormatter;
import uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentInstanceMailMergeFieldFormatter;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterDocumentController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Service
public class LmsDocumentInstanceService {

  private final DocumentInstanceMailMergeFieldFormatter documentMailMergeFieldFormatter;
  private final DocumentInstanceSectionViewService documentInstanceSectionViewService;

  @Autowired
  public LmsDocumentInstanceService(
      DocumentInstanceMailMergeFieldFormatter documentMailMergeFieldFormatter,
      DocumentInstanceSectionViewService documentInstanceSectionViewService
  ) {
    this.documentMailMergeFieldFormatter = documentMailMergeFieldFormatter;
    this.documentInstanceSectionViewService = documentInstanceSectionViewService;
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
}
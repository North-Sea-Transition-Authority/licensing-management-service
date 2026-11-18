package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.FileSource;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.document.instance.LmsDocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.Application;

@Service
public class IssueLettersService {

  private final FileService fileService;

  public IssueLettersService(FileService fileService) {
    this.fileService = fileService;
  }

  @Transactional
  public void saveApplicationLetterToS3(
      DocumentInstanceDto documentInstanceDto,
      Application application,
      ServiceUserDetail serviceUserDetail,
      boolean isPreview,
      LmsDocumentInstanceService lmsDocumentInstanceService,
      String fileUsageType,
      String documentItemType
  ) {
    var sectionsSummaryViews = lmsDocumentInstanceService.getDocumentInstanceSectionsSummaryView(
            documentInstanceDto,
            false,
            application
        )
        .topLevelDocumentInstanceSectionSummaryViews();

    var renderResult = lmsDocumentInstanceService.renderAndSignPdf(
        application,
        isPreview,
        documentInstanceDto,
        sectionsSummaryViews,
        serviceUserDetail
    );

    var pdfContent = renderResult.pdfContent();
    var prefix = isPreview ? "PREVIEW " : "";
    var title = "%s%s.pdf".formatted(prefix, documentInstanceDto.title());

    fileService.upload(builder -> builder
        .withUsage(
            application.getId().toString(),
            fileUsageType,
            documentItemType
        )
        .withFileSource(FileSource.fromInputStreamSource(
            pdfContent::getInputStream,
            title,
            "application/pdf",
            pdfContent.contentLength()
        ))
        .withUploadedBy(serviceUserDetail.wuaId().toString())
        .build());
  }
}

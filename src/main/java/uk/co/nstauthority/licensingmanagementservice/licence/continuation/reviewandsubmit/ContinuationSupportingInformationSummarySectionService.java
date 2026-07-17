package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformation;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformationController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformationFileUsages;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryFileView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class ContinuationSupportingInformationSummarySectionService
    implements SummarySectionService<LicenceContinuationApplicationDetail> {

  public static final String SECTION_NAME = "Additional supporting information";
  public static final int SECTION_DISPLAY_ORDER = 40;

  private final LicenceContinuationSupportingInformationService licenceContinuationSupportingInformationService;
  private final FileService fileService;

  public ContinuationSupportingInformationSummarySectionService(
      LicenceContinuationSupportingInformationService licenceContinuationSupportingInformationService,
      FileService fileService
  ) {
    this.licenceContinuationSupportingInformationService = licenceContinuationSupportingInformationService;
    this.fileService = fileService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user
  ) {
    var summaryItem = getSupportingInformationSummaryItem(licenceContinuationApplicationDetail);
    var summarySection = new SummarySection(SECTION_DISPLAY_ORDER, List.of(summaryItem));
    return Optional.of(summarySection);
  }

  private SummaryItem getSupportingInformationSummaryItem(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var summaryCards = new ArrayList<SummaryCard>();
    summaryCards.add(buildSupportingInformationSummaryCard(licenceContinuationApplicationDetail));
    return SummaryItem.withCards(SECTION_NAME, summaryCards);
  }

  private SummaryCard buildSupportingInformationSummaryCard(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var fileUsages = LicenceContinuationSupportingInformationFileUsages
        .fromApplication(licenceContinuationApplicationDetail);
    var uploadedFiles = fileService.findAll(fileUsages.usageId(), fileUsages.usageType(), fileUsages.documentType());

    var hasSupportingInformation = licenceContinuationSupportingInformationService
        .getSupportingInformation(licenceContinuationApplicationDetail)
        .map(LicenceContinuationSupportingInformation::getHasAdditionalSupportingInformation)
        .orElse(null);

    if (uploadedFiles.isEmpty()) {
      return SummaryCard.simpleSummaryCard(
          SummaryDataView.newBuilder()
              .addStringValue("Has further supporting information", hasSupportingInformation)
              .build()
      );
    }

    var fileViews = uploadedFiles.stream()
        .map(uploadedFile -> toSummaryFileView(uploadedFile, licenceContinuationApplicationDetail))
        .toList();
    return SummaryCard.filesSummaryCardWithHeading(null, fileViews);
  }

  private SummaryFileView toSummaryFileView(
      UploadedFile uploadedFile,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return SummaryFileView.newFromUploadedFile(
        uploadedFile.getKey(),
        uploadedFile,
        ReverseRouter.route(on(LicenceContinuationSupportingInformationController.class).downloadFile(
            uploadedFile.getId(),
            licenceContinuationApplicationDetail.getId(),
            licenceContinuationApplicationDetail,
            null
        ))
    );
  }
}

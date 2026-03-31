package uk.co.nstauthority.licensingmanagementservice.licence.continuation.decision;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentItemType;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.file.FileUsageType;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryFileAndDetailsView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryFileView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;

@Service
public class ContinuationDecisionSummarySectionService {

  public static final String CONTINUATION_LETTER_DETAILS = "Continuation letter details";
  public static final String ISSUED_BY_LABEL = "Issued by";
  public static final String ISSUED_DATE_LABEL = "Issued date";
  public static final String ISSUER_USER_PURPOSE = "Fetch continuation letter issuer for application";
  public static final int SECTION_DISPLAY_ORDER = 10;

  private final FileService fileService;
  private final EnergyPortalUserService energyPortalUserService;

  public ContinuationDecisionSummarySectionService(
      FileService fileService,
      EnergyPortalUserService energyPortalUserService
  ) {
    this.fileService = fileService;
    this.energyPortalUserService = energyPortalUserService;
  }

  public Optional<SummarySection> getSummarySection(
      LicenceContinuationApplicationDetail applicationDetail
  ) {
    var uploadedFiles = fileService.findAll(
        applicationDetail.getLicenceContinuationApplication().getId().toString(),
        FileUsageType.APPLICATION_CONTINUATION_LETTER.getUsageType(),
        DocumentItemType.CONTINUATION_LETTER.name()
    );

    if (uploadedFiles.isEmpty()) {
      return Optional.empty();
    }

    var textData = buildTextSummaryDataView(uploadedFiles.getFirst());

    var fileViews = getSummaryFileViews(
        applicationDetail,
        uploadedFiles
    );

    var mixedData = new SummaryFileAndDetailsView(
        textData,
        fileViews
    );

    var customMixedCard = SummaryCard.filesAndDetailsSummaryCard(
        null,
        mixedData
    );

    var summaryItem = SummaryItem.withCard(CONTINUATION_LETTER_DETAILS, customMixedCard);

    return Optional.of(new SummarySection(
        SECTION_DISPLAY_ORDER,
        List.of(summaryItem)
    ));
  }

  private SummaryDataView buildTextSummaryDataView(UploadedFile issuedLetterFile) {
    var textBuilder = SummaryDataView.newBuilder();

    String uploadedBy = issuedLetterFile.getUploadedBy();
    textBuilder.addStringValue(
        ISSUED_BY_LABEL,
        getIssuerName(Long.valueOf(uploadedBy))
    );

    var issuedDate = issuedLetterFile.getUploadedAt();
    if (issuedDate != null) {
      textBuilder.addStringValue(
          ISSUED_DATE_LABEL,
          DateFormatUtil.convertToDisplayTextWithTime(issuedDate)
      );
    }

    return textBuilder.build();
  }

  private static List<SummaryFileView> getSummaryFileViews(
      LicenceContinuationApplicationDetail applicationDetail,
      List<UploadedFile> uploadedFiles
  ) {
    return uploadedFiles
        .stream()
        .map(file -> SummaryFileView.newFromUploadedFile(
            file.getKey(),
            file,
            ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).downloadLetter(
                applicationDetail.getId(),
                null,
                file.getId(),
                null
            ))
        ))
        .toList();
  }

  private String getIssuerName(Long issuerId) {
    return Optional
        .ofNullable(issuerId)
        .map(wuaId -> WebUserAccountId.from(issuerId))
        .flatMap(webUserAccountId -> energyPortalUserService.findByWuaId(
            webUserAccountId,
            ISSUER_USER_PURPOSE
        ))
        .map(EnergyPortalUserJson::displayName)
        .orElse("Not allocated");
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformation;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationFileUsages;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationHelperService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryFileView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class SupportingInformationSummarySectionService
    implements SummarySectionService<ScheduleWorkProgrammeApplicationDetail> {

  public static final String SUPPORTING_INFORMATION_SUMMARY = "Supporting information";
  private static final String SUPPORTING_INFORMATION_DOCUMENTS = "Supporting information documents";
  public static final String REASON_FOR_REQUEST = "Reason for request";
  public static final String LICENCE_PROGRESS = "Licence progress";
  public static final String IMPACT_ON_DELIVERABLES = "Impact on deliverables";
  public static final String PLAN_DURING_EXTENSION = "Plan during extension";

  public static final int SECTION_DISPLAY_ORDER = 30;

  private final LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService;
  private final LicenceScheduleSupportingInformationHelperService licenceScheduleSupportingInformationHelperService;
  private final FileService fileService;

  public SupportingInformationSummarySectionService(
      LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService,
      LicenceScheduleSupportingInformationHelperService licenceScheduleSupportingInformationHelperService, FileService fileService
  ) {
    this.licenceScheduleSupportingInformationService = licenceScheduleSupportingInformationService;
    this.licenceScheduleSupportingInformationHelperService = licenceScheduleSupportingInformationHelperService;
    this.fileService = fileService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user
  ) {
    List<SummaryItem> summaryItems = new ArrayList<>();

    var detailsItem = getLicenceSummaryItem(scheduleWorkProgrammeApplicationDetail, SUPPORTING_INFORMATION_SUMMARY);
    summaryItems.add(detailsItem);

    var supportingDocumentsSummaryItem = getSupportingDocumentsSummaryItem(scheduleWorkProgrammeApplicationDetail);

    summaryItems.add(supportingDocumentsSummaryItem);

    var summarySection = new SummarySection(SECTION_DISPLAY_ORDER, summaryItems);
    return Optional.of(summarySection);
  }

  public SummaryItem getLicenceSummaryItem(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      String sectionName
  ) {
    var supportingInformation = licenceScheduleSupportingInformationService
        .getRequestByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail);

    if (supportingInformation.isPresent()) {
      var summaryCard = buildSummaryCardFromSupportingInformation(
          supportingInformation.get(),
          scheduleWorkProgrammeApplicationDetail
      );
      return SummaryItem.withCard(sectionName, summaryCard);
    }

    return SummaryItem.withCard(sectionName, null);
  }

  public SummaryItem getSupportingDocumentsSummaryItem(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var summaryCards = new ArrayList<SummaryCard>();
    getSupportingDocumentsSummaryCard(scheduleWorkProgrammeApplicationDetail).ifPresent(summaryCards::add);
    return SummaryItem.withCards(SUPPORTING_INFORMATION_DOCUMENTS, summaryCards);
  }

  private Optional<SummaryCard> getSupportingDocumentsSummaryCard(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    LicenceScheduleSupportingInformationFileUsages fileUsages =
        LicenceScheduleSupportingInformationFileUsages.fromApplication(scheduleWorkProgrammeApplicationDetail);

    var uploadedFiles = fileService.findAll(
        fileUsages.usageId(),
        fileUsages.usageType(),
        fileUsages.documentType()
    );

    if (uploadedFiles.isEmpty()) {
      return Optional.of(SummaryCard.simpleSummaryCard(SummaryDataView.newStringKeyValue(
          "Has supporting documents", "No"
      )));
    }

    var supportingDocumentsFileSummary = getSupportingDocumentsFileSummary(scheduleWorkProgrammeApplicationDetail, uploadedFiles);

    return Optional.of(SummaryCard.filesSummaryCardWithHeading(null, supportingDocumentsFileSummary));
  }

  private List<SummaryFileView> getSupportingDocumentsFileSummary(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      List<UploadedFile> uploadedFiles
  ) {
    return uploadedFiles
        .stream()
        .map(uploadedFile -> SummaryFileView.newFromUploadedFile(
            uploadedFile.getKey(),
            uploadedFile,
            ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).downloadFile(
                uploadedFile.getId(),
                scheduleWorkProgrammeApplicationDetail.getId(),
                scheduleWorkProgrammeApplicationDetail,
                null
            ))
        ))
        .toList();
  }

  private SummaryCard buildSummaryCardFromSupportingInformation(
      LicenceScheduleSupportingInformation licenceScheduleSupportingInformation,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var builder = SummaryDataView.newBuilder();

    builder.addStringValue(REASON_FOR_REQUEST, licenceScheduleSupportingInformation.getReasonForAmendment());
    builder.addStringValue(LICENCE_PROGRESS, licenceScheduleSupportingInformation.getLicenceProgress());

    if (licenceScheduleSupportingInformationHelperService.isExtensionOrAmendment(scheduleWorkProgrammeApplicationDetail)) {
      builder.addStringValue(
          PLAN_DURING_EXTENSION,
          StringUtils.defaultIfBlank(licenceScheduleSupportingInformation.getPlanDuringExtension(), "")
      );
    }

    builder.addStringValue(IMPACT_ON_DELIVERABLES, licenceScheduleSupportingInformation.getImpactOnDeliverables());
    return SummaryCard.simpleSummaryCard(builder.build());
  }
}
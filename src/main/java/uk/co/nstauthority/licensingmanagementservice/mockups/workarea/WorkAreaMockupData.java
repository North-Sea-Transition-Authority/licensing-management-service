package uk.co.nstauthority.licensingmanagementservice.mockups.workarea;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

// Only SWP applications here - neither persona's role grants access to drafts/corrections/continuations.
class WorkAreaMockupData {

  private static final String STEWARD_NAME = "Sarah Mitchell";

  private static final List<ScheduleWorkProgrammeApplicationRow> SCHEDULE_WORK_PROGRAMME_APPLICATIONS = List.of(
      new ScheduleWorkProgrammeApplicationRow(
          "LMS/EAA/2026/44", "P0522", "Fenwick Seaward Resources Ltd", null, Instant.parse("2026-09-13T14:02:00Z")),
      new ScheduleWorkProgrammeApplicationRow(
          "LMS/EAA/2026/41", "P2411", "Kestrel North Sea Energy Ltd", null, Instant.parse("2026-09-11T09:14:00Z")),
      new ScheduleWorkProgrammeApplicationRow(
          "LMS/EAA/2026/38", "P1876", "Aurora Petroleum Group plc", STEWARD_NAME, Instant.parse("2026-09-08T11:47:00Z")),
      new ScheduleWorkProgrammeApplicationRow(
          "LMS/EAA/2026/33", "P3350", "Silverburn Exploration Ltd", "Priya Anand", Instant.parse("2026-09-06T10:05:00Z")),
      new ScheduleWorkProgrammeApplicationRow(
          "LMS/EAA/2026/29", "P0847", "Meridian Offshore Ltd", "James Okafor", Instant.parse("2026-09-02T16:30:00Z"))
  );

  private record ScheduleWorkProgrammeApplicationRow(
      String applicationReference, String licenceReference, String licensee, String stewardName, Instant submittedDatetime
  ) {
  }

  // No per-case reviewer assignment, so this list is both tabs for the Continuation reviewer persona.
  private static final List<ContinuationApplicationRow> CONTINUATION_APPLICATIONS = List.of(
      new ContinuationApplicationRow(
          "LMS/CA/2026/12", "P1204", "Brentford Seaward Ltd", Instant.parse("2026-09-09T13:20:00Z")),
      new ContinuationApplicationRow(
          "LMS/CA/2026/15", "P0733", "Kestrel North Sea Energy Ltd", Instant.parse("2026-09-05T10:00:00Z"))
  );

  private record ContinuationApplicationRow(
      String applicationReference, String licenceReference, String licensee, Instant submittedDatetime
  ) {
  }

  // Schedule administrator isn't licence-type scoped, so this spans offshore/onshore/carbon storage.
  private static final List<DraftScheduleRow> DRAFT_SCHEDULES = List.of(
      new DraftScheduleRow("P2044", "Northgate Energy Holdings Ltd", Instant.parse("2026-09-10T08:00:00Z")),
      new DraftScheduleRow("CS077", "Meridian Carbon Storage Ltd", Instant.parse("2026-09-04T08:00:00Z")),
      new DraftScheduleRow("PEDL045", "Ridgeway Onshore Ltd", Instant.parse("2026-09-12T08:00:00Z"))
  );

  private record DraftScheduleRow(String licenceReference, String licensee, Instant createdDatetime) {
  }

  private WorkAreaMockupData() {
  }

  static List<SearchResultItem> getStewardMyWorkItems(String linkUrl) {
    return sortedByTransactionDatetime(SCHEDULE_WORK_PROGRAMME_APPLICATIONS.stream()
        .filter(row -> STEWARD_NAME.equals(row.stewardName()))
        .map(row -> scheduleWorkProgrammeApplication(linkUrl, row))
        .toList());
  }

  // A case manager's job is allocating a steward, so unassigned cases are theirs to act on.
  static List<SearchResultItem> getCaseManagerMyWorkItems(String linkUrl) {
    return sortedByTransactionDatetime(SCHEDULE_WORK_PROGRAMME_APPLICATIONS.stream()
        .filter(row -> row.stewardName() == null)
        .map(row -> scheduleWorkProgrammeApplication(linkUrl, row))
        .toList());
  }

  static List<SearchResultItem> getAllWorkItems(String linkUrl) {
    return sortedByTransactionDatetime(SCHEDULE_WORK_PROGRAMME_APPLICATIONS.stream()
        .map(row -> scheduleWorkProgrammeApplication(linkUrl, row))
        .toList());
  }

  static List<SearchResultItem> getContinuationReviewerItems(String linkUrl) {
    return sortedByTransactionDatetime(CONTINUATION_APPLICATIONS.stream()
        .map(row -> continuationApplication(linkUrl, row))
        .toList());
  }

  static List<SearchResultItem> getScheduleAdministratorItems(String linkUrl) {
    return sortedByTransactionDatetime(DRAFT_SCHEDULES.stream()
        .map(row -> draftSchedule(linkUrl, row))
        .toList());
  }

  private static List<SearchResultItem> sortedByTransactionDatetime(List<SearchResultItem> items) {
    return items.stream()
        .sorted(Comparator.comparing(SearchResultItem::transactionDatetime).reversed())
        .toList();
  }

  private static SearchResultItem scheduleWorkProgrammeApplication(String linkUrl, ScheduleWorkProgrammeApplicationRow row) {
    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Licence", row.licenceReference())
        .addStringValue("Licensees", row.licensee())
        .addStringValue("Status", "Submitted")
        .addStringValue("Steward", row.stewardName() != null ? row.stewardName() : "Not allocated")
        .build();

    return SearchResultItem.newBuilder()
        .withId(row.applicationReference())
        .withLinkHeadingText("%s - %s".formatted(
            row.applicationReference(), ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getDisplayName().toLowerCase()))
        .withLinkHeadingUrl(linkUrl)
        .withCaptionText("Submitted %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(row.submittedDatetime())))
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(row.submittedDatetime())
        .build();
  }

  private static SearchResultItem continuationApplication(String linkUrl, ContinuationApplicationRow row) {
    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Licence", row.licenceReference())
        .addStringValue("Licensees", row.licensee())
        .addStringValue("Status", "Submitted")
        .build();

    return SearchResultItem.newBuilder()
        .withId(row.applicationReference())
        .withLinkHeadingText("%s - %s".formatted(
            row.applicationReference(), ApplicationType.CONTINUATION_APPLICATION.getDisplayName().toLowerCase()))
        .withLinkHeadingUrl(linkUrl)
        .withCaptionText("Submitted %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(row.submittedDatetime())))
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(row.submittedDatetime())
        .build();
  }

  private static SearchResultItem draftSchedule(String linkUrl, DraftScheduleRow row) {
    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Licence", row.licenceReference())
        .addStringValue("Licensees", row.licensee())
        .build();

    return SearchResultItem.newBuilder()
        .withId(row.licenceReference())
        .withLinkHeadingText("%s - draft schedule".formatted(row.licenceReference()))
        .withLinkHeadingUrl(linkUrl)
        .withCaptionText("Created %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(row.createdDatetime())))
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(row.createdDatetime())
        .build();
  }
}

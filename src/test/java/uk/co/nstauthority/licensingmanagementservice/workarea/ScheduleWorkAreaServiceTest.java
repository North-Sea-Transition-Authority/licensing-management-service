package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkAreaServiceTest {

  @Mock
  LicenceScheduleDetailService licenceScheduleDetailService;

  @Mock
  LicenceSearchService licenceSearchService;

  @Mock
  WorkAreaItemViewService workAreaItemViewService;

  @InjectMocks
  ScheduleWorkAreaService scheduleWorkAreaService;

  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

  @Test
  void getWorkAreaItemsUnfiltered() {
    var serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var testInstant = Instant.now(clock);

    var licence1 = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS001")
        .build();
    var licenceSchedule1 = LicenceScheduleTestUtil.createLicenceSchedule(licence1);
    var licenceScheduleDetail1 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule1)
        .withId(UUID.randomUUID())
        .withCreatedInstant(testInstant)
        .build();

    var licence2 = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS002")
        .build();
    var licenceSchedule2 = LicenceScheduleTestUtil.createLicenceSchedule(licence2);
    var licenceScheduleDetail2 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule2)
        .withId(UUID.randomUUID())
        .withCreatedInstant(testInstant.minus(1, ChronoUnit.HOURS))
        .build();

    when(licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(serviceUserDetail)).thenReturn(
        List.of(licenceScheduleDetail1, licenceScheduleDetail2)
    );
    when(workAreaItemViewService.getWorkAreaItemLogsForUser(any(), any())).thenReturn(List.of());

    var org1 = "Org 1";
    var org2 = "Org 2";
    var orgList1 = List.of(org1, org2);
    var orgList2 = List.of(org1);
    var licenceResponsibleOrgMap = Map.of(licence1, orgList1, licence2, orgList2);
    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence1, licence2))).thenReturn(
        licenceResponsibleOrgMap
    );

    var workAreaItems = scheduleWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(
            SearchResultItem::id,
            SearchResultItem::linkHeadingText,
            SearchResultItem::linkHeadingUrl,
            SearchResultItem::dataItemRows,
            SearchResultItem::captionText
        )
        .containsExactly(
            tuple(
                licenceScheduleDetail1.getId().toString(),
                String.format("%s - draft schedule", licence1.getLicenceReference()),
                ReverseRouter.route(on(LicenceScheduleTimelineController.class)
                    .renderLicenceScheduleTimeline(licenceScheduleDetail1.getId(), null, null, null)),
                List.of(SummaryDataView.newBuilder()
                    .addStringValue("Licence", licence1.getLicenceReference())
                    .addStringValue("Licensees", String.join(", ", orgList1))
                    .build()),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(licenceScheduleDetail1.getCreatedInstant()))
            ),
            tuple(
                licenceScheduleDetail2.getId().toString(),
                String.format("%s - draft schedule", licence2.getLicenceReference()),
                ReverseRouter.route(on(LicenceScheduleTimelineController.class)
                    .renderLicenceScheduleTimeline(licenceScheduleDetail2.getId(), null, null, null)),
            List.of(SummaryDataView.newBuilder()
                .addStringValue("Licence", licence2.getLicenceReference())
                .addStringValue("Licensees", String.join(", ", orgList2))
                .build()),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(licenceScheduleDetail2.getCreatedInstant()))
            )
        );
  }
  @Test
  void getWorkAreaItems_filteredByLicenceReference() {
    var serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var testInstant = Instant.now(clock);

    var licence1 = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS001")
        .build();
    var licenceSchedule1 = LicenceScheduleTestUtil.createLicenceSchedule(licence1);
    var licenceScheduleDetail1 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule1)
        .withId(UUID.randomUUID())
        .withCreatedInstant(testInstant)
        .build();

    var licence2 = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS002")
        .build();
    var licenceSchedule2 = LicenceScheduleTestUtil.createLicenceSchedule(licence2);
    var licenceScheduleDetail2 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule2)
        .withId(UUID.randomUUID())
        .withCreatedInstant(testInstant)
        .build();

    when(licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(serviceUserDetail)).thenReturn(
        List.of(licenceScheduleDetail1, licenceScheduleDetail2)
    );
    when(workAreaItemViewService.getWorkAreaItemLogsForUser(any(), any())).thenReturn(List.of());

    var org1 = "Org 1";
    var licenceResponsibleOrgMap = Map.of(licence2, List.of(org1));
    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence2))).thenReturn(
        licenceResponsibleOrgMap
    );

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setLicenceReference("2");
    var workAreaItems = scheduleWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(
            SearchResultItem::id,
            SearchResultItem::linkHeadingText,
            SearchResultItem::linkHeadingUrl,
            SearchResultItem::dataItemRows,
            SearchResultItem::captionText
        )
        .containsExactly(
            tuple(
                licenceScheduleDetail2.getId().toString(),
                String.format("%s - draft schedule", licence2.getLicenceReference()),
                ReverseRouter.route(on(LicenceScheduleTimelineController.class)
                    .renderLicenceScheduleTimeline(licenceScheduleDetail2.getId(), null, null, null)),
                List.of(SummaryDataView.newBuilder()
                    .addStringValue("Licence", licence2.getLicenceReference())
                    .addStringValue("Licensees", String.join(", ", org1))
                    .build()),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(licenceScheduleDetail2.getCreatedInstant()))
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByLicenceType() {
    var serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var testInstant = Instant.now(clock);

    var licence1 = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS001")
        .build();
    var licenceScheduleDetail1 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(
            LicenceScheduleTestUtil.createLicenceSchedule(licence1))
        .withId(UUID.randomUUID())
        .withCreatedInstant(testInstant)
        .build();

    var licence2 = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference("P002")
        .build();
    var licenceScheduleDetail2 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(
            LicenceScheduleTestUtil.createLicenceSchedule(licence2))
        .withId(UUID.randomUUID())
        .withCreatedInstant(testInstant)
        .build();

    when(licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(serviceUserDetail)).thenReturn(
        List.of(licenceScheduleDetail1, licenceScheduleDetail2)
    );
    when(workAreaItemViewService.getWorkAreaItemLogsForUser(any(), any())).thenReturn(List.of());
    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence2))).thenReturn(
        Map.of(licence2, List.of())
    );

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setLicenceTypes(List.of(LicenceType.SEAWARD_PRODUCTION.name()));
    var workAreaItems = scheduleWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::id)
        .containsExactly(licenceScheduleDetail2.getId().toString());
  }

  @Test
  void getWorkAreaItems_whenApplicationReferenceFilterApplied_thenExcluded() {
    var serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var licence = LicenceTestUtil.builder().withId(1).withLicenceType(LicenceType.CARBON_STORAGE).withLicenceReference("CS001").build();
    var detail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(LicenceScheduleTestUtil.createLicenceSchedule(licence))
        .withId(UUID.randomUUID()).withCreatedInstant(Instant.now(clock)).build();

    when(licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(serviceUserDetail)).thenReturn(List.of(detail));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationReference("LMS/EEA/001");
    var workAreaItems = scheduleWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems).isEmpty();
  }

  @Test
  void getWorkAreaItems_whenApplicationTypeFilterApplied_thenExcluded() {
    var serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var licence = LicenceTestUtil.builder().withId(1).withLicenceType(LicenceType.CARBON_STORAGE).withLicenceReference("CS001").build();
    var detail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(LicenceScheduleTestUtil.createLicenceSchedule(licence))
        .withId(UUID.randomUUID()).withCreatedInstant(Instant.now(clock)).build();

    when(licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(serviceUserDetail)).thenReturn(List.of(detail));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationTypes(List.of(ApplicationType.CONTINUATION_APPLICATION.name()));
    var workAreaItems = scheduleWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems).isEmpty();
  }

  @Test
  void getWorkAreaItems_whenApplicationStatusFilterApplied_thenExcluded() {
    var serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var licence = LicenceTestUtil.builder().withId(1).withLicenceType(LicenceType.CARBON_STORAGE).withLicenceReference("CS001").build();
    var detail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(LicenceScheduleTestUtil.createLicenceSchedule(licence))
        .withId(UUID.randomUUID()).withCreatedInstant(Instant.now(clock)).build();

    when(licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(serviceUserDetail)).thenReturn(List.of(detail));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationStatuses(List.of(ApplicationStatus.DRAFT.name()));
    var workAreaItems = scheduleWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems).isEmpty();
  }

  @Test
  void getWorkAreaItems_whenItemNotYetViewed_showsNewBadge() {
    var serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var testInstant = Instant.now(clock);
    var licence = LicenceTestUtil.builder().withId(1).withLicenceType(LicenceType.CARBON_STORAGE).withLicenceReference("CS001").build();
    var detail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(LicenceScheduleTestUtil.createLicenceSchedule(licence))
        .withId(UUID.randomUUID()).withCreatedInstant(testInstant).build();

    when(licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(serviceUserDetail)).thenReturn(List.of(detail));
    when(workAreaItemViewService.getWorkAreaItemLogsForUser(any(), any())).thenReturn(List.of());
    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence))).thenReturn(Map.of(licence, List.of()));

    var workAreaItems = scheduleWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::hasNewLabel)
        .containsExactly(true);

    // View logs are fetched once per request (batched), never per item
    verify(workAreaItemViewService, times(1)).getWorkAreaItemLogsForUser(any(), any());
    verify(workAreaItemViewService, never()).hasUserViewedItem(any());
  }

  @Test
  void getWorkAreaItems_whenItemAlreadyViewed_doesNotShowNewBadge() {
    var serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var testInstant = Instant.now(clock);
    var licence = LicenceTestUtil.builder().withId(1).withLicenceType(LicenceType.CARBON_STORAGE).withLicenceReference("CS001").build();
    var detail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(LicenceScheduleTestUtil.createLicenceSchedule(licence))
        .withId(UUID.randomUUID()).withCreatedInstant(testInstant).build();

    when(licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(serviceUserDetail)).thenReturn(List.of(detail));
    when(workAreaItemViewService.getWorkAreaItemLogsForUser(any(), any()))
        .thenReturn(List.of(new WorkAreaItemView(
            detail.getId(),
            WorkAreaDataItemType.DRAFT_LICENCE_SCHEDULE,
            serviceUserDetail.wuaId()
        )));
    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence))).thenReturn(Map.of(licence, List.of()));

    var workAreaItems = scheduleWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::hasNewLabel)
        .containsExactly(false);
  }
}

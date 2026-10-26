package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@ExtendWith(MockitoExtension.class)
class WorkAreaServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  @Mock
  private ScheduleWorkAreaService scheduleWorkAreaService;

  @Mock
  private ScheduleAndWorkProgrammeApplicationWorkAreaService scheduleAndWorkProgrammeApplicationWorkAreaService;

  @Mock
  private WorkAreaContextService workAreaContextService;

  private WorkAreaService workAreaService;

  private final WorkAreaFilterForm form = new WorkAreaFilterForm();
  private final ServiceUserDetail user = ServiceUserDetailTestUtil.newBuilder().build();
  private final WorkAreaContext context = WorkAreaContextTestUtil.builder().build();

  @BeforeEach
  void setUp() {
    var environment = new MockEnvironment();
    environment.setActiveProfiles("enable-lms1", "enable-lms2");
    workAreaService = new WorkAreaService(
        List.of(scheduleWorkAreaService, scheduleAndWorkProgrammeApplicationWorkAreaService),
        new FeatureFlagService(environment),
        workAreaContextService
    );
    when(scheduleWorkAreaService.getReleaseFeature()).thenReturn(ReleaseFeature.MANAGE_SCHEDULE);
    when(scheduleAndWorkProgrammeApplicationWorkAreaService.getReleaseFeature())
        .thenReturn(ReleaseFeature.SCHEDULE_APPLICATION);
  }

  @Test
  void getAllWorkResults_buildsEverySectionFromOneSharedContext_newestFirst() {
    var oldestItem = item(NOW.minus(2L, ChronoUnit.DAYS));
    var middleItem = item(NOW.minus(1L, ChronoUnit.DAYS));
    var newestItem = item(NOW);
    var scheduleSection = section(WorkAreaDataItemType.DRAFT_LICENCE_SCHEDULE, List.of(oldestItem, newestItem));
    var applicationSection = section(WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION, List.of(middleItem));
    when(scheduleWorkAreaService.loadSection(form, user, WorkAreaScope.ALL_WORK)).thenReturn(scheduleSection);
    when(scheduleAndWorkProgrammeApplicationWorkAreaService.loadSection(form, user, WorkAreaScope.ALL_WORK)).thenReturn(applicationSection);
    when(workAreaContextService.buildContext(List.of(scheduleSection, applicationSection), form, user))
        .thenReturn(context);

    var workAreaResults = workAreaService.getAllWorkResults(form, user);

    assertThat(workAreaResults).containsExactly(newestItem, middleItem, oldestItem);
  }

  @Test
  void getMyWorkResults_loadsEverySectionForMyWork() {
    var scheduleSection = section(WorkAreaDataItemType.DRAFT_LICENCE_SCHEDULE, List.of());
    var applicationSection = section(WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION, List.of());
    when(scheduleWorkAreaService.loadSection(form, user, WorkAreaScope.MY_WORK)).thenReturn(scheduleSection);
    when(scheduleAndWorkProgrammeApplicationWorkAreaService.loadSection(form, user, WorkAreaScope.MY_WORK)).thenReturn(applicationSection);
    when(workAreaContextService.buildContext(List.of(scheduleSection, applicationSection), form, user))
        .thenReturn(context);

    workAreaService.getMyWorkResults(form, user);

    verify(scheduleWorkAreaService).loadSection(form, user, WorkAreaScope.MY_WORK);
    verify(scheduleAndWorkProgrammeApplicationWorkAreaService).loadSection(form, user, WorkAreaScope.MY_WORK);
  }

  private WorkAreaSection section(WorkAreaDataItemType itemType, List<SearchResultItem> items) {
    return new WorkAreaSection(itemType, List.of(), OrganisationGroupLookup.NOT_REQUIRED, sharedContext -> {
      assertThat(sharedContext).isSameAs(context);
      return items;
    });
  }

  private static SearchResultItem item(Instant transactionDatetime) {
    return SearchResultItem.newBuilder().withTransactionDatetime(transactionDatetime).build();
  }
}

package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;

@ExtendWith(MockitoExtension.class)
class WorkAreaServiceTest {

  private static final Instant INSTANT = Instant.now();

  @Mock
  ScheduleWorkAreaService scheduleWorkAreaService;

  Clock clock;

  WorkAreaService workAreaService;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.from(INSTANT), ZoneId.systemDefault());
    workAreaService = new WorkAreaService(List.of(scheduleWorkAreaService));
  }

  @Test
  void getWorkAreaItems() {
    var form = new WorkAreaFilterForm();
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var oldestItem = SearchResultItem.newBuilder().withTransactionDatetime(clock.instant().minus(2L, ChronoUnit.DAYS)).build();
    var newestItem = SearchResultItem.newBuilder().withTransactionDatetime(clock.instant()).build();

    when(scheduleWorkAreaService.getWorkAreaItems(form, user)).thenReturn(List.of(oldestItem, newestItem));

    var workAreaResults = workAreaService.getWorkAreaResults(form, user);

    assertThat(workAreaResults).containsExactly(newestItem, oldestItem);

  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationApplicationTaskListServiceTest {

  @Mock
  private LicenceContinuationApplicationTaskListSectionService licenceContinuationApplicationTaskListSectionService;

  private LicenceContinuationApplicationTaskListService licenceContinuationApplicationTaskListService;

    @BeforeEach
    void setUp() {
      List<TaskListSectionService<LicenceContinuationApplicationDetail>> taskListSections = List.of(
          licenceContinuationApplicationTaskListSectionService
      );
      licenceContinuationApplicationTaskListService = new LicenceContinuationApplicationTaskListService(taskListSections);
    }

    @Test
    void getAllSections() {
      var continuationApplicationDetail = new LicenceContinuationApplicationDetail();
      var user = ServiceUserDetailTestUtil.newBuilder().build();
      var section1 = Optional.of(new TaskListSection("Application details", 10, List.of()));
      when(licenceContinuationApplicationTaskListSectionService.getSection(continuationApplicationDetail, user)).thenReturn(section1);

      var sections = licenceContinuationApplicationTaskListService.getAllSections(continuationApplicationDetail, user);
      assertThat(sections).containsExactly(section1.get());
    }
}
package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney.LicenceContinuationExternalContributorController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney.LicenceContinuationExternalContributorService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationApplicationTaskListSectionServiceTest {

  @Mock
  private LicenceContinuationExternalContributorService licenceContinuationExternalContributorService;

  @InjectMocks
  private LicenceContinuationApplicationTaskListSectionService licenceContinuationApplicationTaskListSectionService;

  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;
  private ServiceUserDetail user;

  @BeforeEach
  void setUp() {
    user = ServiceUserDetailTestUtil
        .newBuilder().build();

    var licenceContinuationApplication = new LicenceContinuationApplication();
    licenceContinuationApplication.setId(UUID.randomUUID());

    this.licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil
        .builder()
        .withId(UUID.randomUUID())
        .withLicenceContinuationApplication(licenceContinuationApplication)
        .build();
  }

  @Test
  void getSection_whenQuestionAnswered_itemIsComplete() {
    when(licenceContinuationExternalContributorService
        .isExternalContributorSectionComplete(licenceContinuationApplicationDetail))
        .thenReturn(true);

    var sectionOptional = licenceContinuationApplicationTaskListSectionService.getSection(
        licenceContinuationApplicationDetail, user);
    assertThat(sectionOptional).isPresent();
    var section = sectionOptional.get();

    assertThat(section)
        .extracting(
            TaskListSection::items,
            TaskListSection::displayName,
            TaskListSection::displayOrder
        )
        .containsExactly(
            List.of(
                new TaskListItem(
                    LicenceContinuationApplicationTaskListSectionService.EXTERNAL_CONTRIBUTORS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(LicenceContinuationExternalContributorController.class)
                        .renderForm(licenceContinuationApplicationDetail.getId(), null))
                )
            ),
            LicenceContinuationApplicationTaskListSectionService.LICENCE_CONTINUATION_DETAILS_SECTION_NAME,
            LicenceContinuationApplicationTaskListSectionService.SECTION_ORDER
        );
  }

  @Test
  void getSection_whenQuestionNotAnswered_itemIsNotComplete() {
    when(licenceContinuationExternalContributorService
        .isExternalContributorSectionComplete(licenceContinuationApplicationDetail))
        .thenReturn(false);

    var sectionOptional = licenceContinuationApplicationTaskListSectionService.getSection(
        licenceContinuationApplicationDetail, user);
    assertThat(sectionOptional).isPresent();

    assertThat(sectionOptional.get().items())
        .containsExactly(
            new TaskListItem(
                LicenceContinuationApplicationTaskListSectionService.EXTERNAL_CONTRIBUTORS,
                TaskListLabel.NOT_COMPLETE,
                ReverseRouter.route(on(LicenceContinuationExternalContributorController.class)
                    .renderForm(licenceContinuationApplicationDetail.getId(), null))
            )
        );
  }
}

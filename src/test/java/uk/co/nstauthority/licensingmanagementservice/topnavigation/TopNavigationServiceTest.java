package uk.co.nstauthority.licensingmanagementservice.topnavigation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;

@ExtendWith(MockitoExtension.class)
class TopNavigationServiceTest {

  @Mock
  private UserDetailService userDetailService;

  @InjectMocks
  private TopNavigationService topNavigationService;

  @Test
  void getTopNavigationItems_loggedInUser() {
    when(userDetailService.isUserLoggedIn()).thenReturn(true);
    var topNavigationItems = topNavigationService.getTopNavigationItems();
    assertThat(topNavigationItems).containsExactly(
        TopNavigationItem.WORK_AREA,
        TopNavigationItem.LICENCES,
        TopNavigationItem.TEAMS,
        TopNavigationItem.DOCUMENT_LIBRARY
    );
  }

  @Test
  void getTopNavigationItems_withoutLoggedInUser() {
    when(userDetailService.isUserLoggedIn()).thenReturn(false);
    var topNavigationItems = topNavigationService.getTopNavigationItems();
    assertThat(topNavigationItems).isEmpty();
  }

}
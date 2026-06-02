package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import uk.co.fivium.energyportal.accounts.epmq.messages.UserDto;

@ExtendWith(MockitoExtension.class)
class UserUpdatedHandlerTest {

  private static final long WUA_ID = 100L;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @InjectMocks
  private UserUpdatedHandler userUpdatedHandler;

  @Test
  void accept_whenNotCancelled_doesNotPublishEvent() {
    var userDto = mock(UserDto.class);
    when(userDto.isCancelled()).thenReturn(false);

    userUpdatedHandler.accept(userDto);

    verifyNoInteractions(applicationEventPublisher);
  }

  @Test
  void accept_whenCancelled_publishesUserCancelledEvent() {
    var userDto = mock(UserDto.class);
    when(userDto.isCancelled()).thenReturn(true);
    when(userDto.wuaId()).thenReturn(WUA_ID);

    userUpdatedHandler.accept(userDto);

    verify(applicationEventPublisher).publishEvent(new UserCancelledEvent(WUA_ID));
  }
}

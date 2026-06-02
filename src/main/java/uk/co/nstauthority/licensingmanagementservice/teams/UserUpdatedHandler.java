package uk.co.nstauthority.licensingmanagementservice.teams;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import uk.co.fivium.energyportal.accounts.epmq.messages.UserDto;
import uk.co.fivium.energyportal.starter.accounts.EnergyPortalUserUpdatedConsumer;

@Component
public class UserUpdatedHandler implements EnergyPortalUserUpdatedConsumer {

  private final ApplicationEventPublisher applicationEventPublisher;

  UserUpdatedHandler(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void accept(UserDto userDto) {
    if (userDto.isCancelled()) {
      applicationEventPublisher.publishEvent(new UserCancelledEvent(userDto.wuaId()));
    }
  }
}

package uk.co.nstauthority.licensingmanagementservice.energyportal;

import org.springframework.stereotype.Component;
import uk.co.fivium.energyportal.starter.LogCorrelationIdSupplier;
import uk.co.fivium.energyportalmessagequeue.util.CorrelationIdUtil;

@Component
class EnergyPortalAccountsLogCorrelationIdSupplier implements LogCorrelationIdSupplier {

  @Override
  public String get() {
    return CorrelationIdUtil.getCorrelationIdFromMdc();
  }

}

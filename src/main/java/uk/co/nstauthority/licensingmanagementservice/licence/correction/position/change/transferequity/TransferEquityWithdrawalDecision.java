package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum TransferEquityWithdrawalDecision implements Displayable {
  WITHDRAW("Yes, remove them from the position", 10),
  RETAIN("No, this organisation will retain a beneficial interest in the licence position despite holding no equity", 20);

  private final String displayName;
  private final int displayOrder;

  TransferEquityWithdrawalDecision(String displayName, int displayOrder) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  public boolean retainsBeneficialInterest() {
    return this == RETAIN;
  }

  public static Map<String, String> getOptions() {
    return DisplayableEnumOptionUtil.getDisplayableOptions(TransferEquityWithdrawalDecision.class);
  }
}
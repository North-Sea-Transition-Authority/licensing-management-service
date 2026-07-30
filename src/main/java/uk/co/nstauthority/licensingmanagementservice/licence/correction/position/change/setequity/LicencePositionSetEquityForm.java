package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity;

import uk.co.fivium.formlibrary.input.DecimalInput;

public class LicencePositionSetEquityForm {

  private String transferTo;
  private final DecimalInput equity = new DecimalInput("equity", "the equity to set");

  public String getTransferTo() {
    return transferTo;
  }

  public void setTransferTo(String transferTo) {
    this.transferTo = transferTo;
  }

  public DecimalInput getEquity() {
    return equity;
  }
}
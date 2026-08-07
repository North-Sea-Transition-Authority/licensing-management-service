package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity;

import uk.co.fivium.formlibrary.input.DecimalInput;

public class LicencePositionTransferEquityForm {

  private String transferFrom;
  private String transferTo;
  private final DecimalInput equity = new DecimalInput("equity", "the equity to transfer");

  public String getTransferFrom() {
    return transferFrom;
  }

  public void setTransferFrom(String transferFrom) {
    this.transferFrom = transferFrom;
  }

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
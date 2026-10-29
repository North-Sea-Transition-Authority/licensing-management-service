package uk.co.nstauthority.licensingmanagementservice.mockups.licencecorrection.operator;

public class OperatorCorrectionForm {

  private String changeType;

  private String operatorId;

  private String blockId;

  private String subareaId;

  public String getChangeType() {
    return changeType;
  }

  public void setChangeType(String changeType) {
    this.changeType = changeType;
  }

  public String getOperatorId() {
    return operatorId;
  }

  public void setOperatorId(String operatorId) {
    this.operatorId = operatorId;
  }

  public String getBlockId() {
    return blockId;
  }

  public void setBlockId(String blockId) {
    this.blockId = blockId;
  }

  public String getSubareaId() {
    return subareaId;
  }

  public void setSubareaId(String subareaId) {
    this.subareaId = subareaId;
  }
}
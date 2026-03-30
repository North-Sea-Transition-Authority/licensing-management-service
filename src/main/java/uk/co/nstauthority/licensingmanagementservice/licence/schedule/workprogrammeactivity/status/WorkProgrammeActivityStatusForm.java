package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

public class WorkProgrammeActivityStatusForm {

  private WorkProgrammeStatus status;

  private String transferredToLicenceId;

  public WorkProgrammeStatus getStatus() {
    return status;
  }

  public void setStatus(WorkProgrammeStatus status) {
    this.status = status;
  }

  public String getTransferredToLicenceId() {
    return transferredToLicenceId;
  }

  public void setTransferredToLicenceId(String transferredToLicenceId) {
    this.transferredToLicenceId = transferredToLicenceId;
  }
}

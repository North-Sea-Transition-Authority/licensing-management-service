package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Audited
@Entity(name = "licence_schedule_supporting_information")
public class LicenceScheduleSupportingInformation {

  @Id
  @UuidGenerator
  private UUID id;

  private String licenceProgress;

  private String reasonForAmendment;

  private String planDuringExtension;

  private String impactOnDeliverables;

  @ManyToOne
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails;

  public UUID getId() {
    return id;
  }

  public void setId(UUID requestId) {
    this.id = requestId;
  }

  public String getLicenceProgress() {
    return licenceProgress;
  }

  public void setLicenceProgress(String licenceProgress) {
    this.licenceProgress = licenceProgress;
  }

  public String getReasonForAmendment() {
    return reasonForAmendment;
  }

  public void setReasonForAmendment(String reasonForAmendment) {
    this.reasonForAmendment = reasonForAmendment;
  }

  public String getPlanDuringExtension() {
    return planDuringExtension;
  }

  public void setPlanDuringExtension(String planDuringExtension) {
    this.planDuringExtension = planDuringExtension;
  }

  public String getImpactOnDeliverables() {
    return impactOnDeliverables;
  }

  public void setImpactOnDeliverables(String impactOnDeliverables) {
    this.impactOnDeliverables = impactOnDeliverables;
  }

  public ScheduleWorkProgrammeApplicationDetail getScheduleWorkProgrammeApplicationDetails() {
    return scheduleWorkProgrammeApplicationDetails;
  }

  public void setScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails) {
    this.scheduleWorkProgrammeApplicationDetails = scheduleWorkProgrammeApplicationDetails;
  }
}
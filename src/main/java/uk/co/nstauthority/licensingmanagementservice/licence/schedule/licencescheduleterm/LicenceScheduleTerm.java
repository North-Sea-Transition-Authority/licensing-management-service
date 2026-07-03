package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.duplication.LinkedToDuplicationParent;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

@Audited
@Entity
@Table(name = "licence_schedule_terms")
@DiscriminatorValue("TERM")
public class LicenceScheduleTerm extends ScheduleEvent implements LinkedToDuplicationParent<LicenceScheduleDetail> {

  @ManyToOne
  @JoinColumn(name = "licence_schedule_detail_id")
  private LicenceScheduleDetail licenceScheduleDetail;

  @Enumerated(EnumType.STRING)
  private TermType termType;

  @Embedded
  @AttributeOverride(name = "days", column = @Column(name = "term_duration_days"))
  @AttributeOverride(name = "months", column = @Column(name = "term_duration_months"))
  @AttributeOverride(name = "years", column = @Column(name = "term_duration_years"))
  private ThreeFieldDuration termDuration;

  private LocalDate startDate;

  private LocalDate endDate;

  public LicenceScheduleDetail getLicenceScheduleDetail() {
    return licenceScheduleDetail;
  }

  public void setLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail) {
    this.licenceScheduleDetail = licenceScheduleDetail;
  }

  @Override
  public void setDuplicationParent(LicenceScheduleDetail licenceScheduleDetail) {
    setLicenceScheduleDetail(licenceScheduleDetail);
  }

  public TermType getTermType() {
    return termType;
  }

  public void setTermType(TermType termType) {
    this.termType = termType;
  }

  public ThreeFieldDuration getTermDuration() {
    return termDuration;
  }

  public void setTermDuration(ThreeFieldDuration termDuration) {
    this.termDuration = termDuration;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.TERM;
  }

  @Override
  public String getEventCaption() {
    return termType.getDisplayName();
  }
}

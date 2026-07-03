package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.duplication.LinkedToDuplicationParent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

@Audited
@Entity
@Table(name = "licence_schedule_rates")
@DiscriminatorValue("RATE")
public class LicenceScheduleRate extends ScheduleEvent implements LinkedToDuplicationParent<LicenceScheduleDetail> {

  @ManyToOne
  @JoinColumn(name = "licence_schedule_detail_id")
  private LicenceScheduleDetail licenceScheduleDetail;

  @Enumerated(value = EnumType.STRING)
  private RateDefinitionOption rateDefinitionOption;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_term_id")
  private LicenceScheduleTerm licenceScheduleTerm;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_phase_id")
  private LicenceSchedulePhase licenceSchedulePhase;

  @Enumerated(value = EnumType.STRING)
  private RateRelativeDateOption rateRelativeDateOption;

  @Embedded
  @AttributeOverride(name = "days", column = @Column(name = "relative_duration_days"))
  @AttributeOverride(name = "months", column = @Column(name = "relative_duration_months"))
  @AttributeOverride(name = "years", column = @Column(name = "relative_duration_years"))
  private ThreeFieldDuration relativeDuration;

  private LocalDate startDate;

  private BigDecimal rentalRate;

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

  public RateDefinitionOption getRateDefinitionOption() {
    return rateDefinitionOption;
  }

  public void setRateDefinitionOption(RateDefinitionOption rateDefinitionOption) {
    this.rateDefinitionOption = rateDefinitionOption;
  }

  public LicenceScheduleTerm getLicenceScheduleTerm() {
    return licenceScheduleTerm;
  }

  public void setLicenceScheduleTerm(LicenceScheduleTerm licenceScheduleTerm) {
    this.licenceScheduleTerm = licenceScheduleTerm;
  }

  public LicenceSchedulePhase getLicenceSchedulePhase() {
    return licenceSchedulePhase;
  }

  public void setLicenceSchedulePhase(LicenceSchedulePhase licenceSchedulePhase) {
    this.licenceSchedulePhase = licenceSchedulePhase;
  }

  public RateRelativeDateOption getRateRelativeDateOption() {
    return rateRelativeDateOption;
  }

  public void setRateRelativeDateOption(RateRelativeDateOption rateRelativeDateOption) {
    this.rateRelativeDateOption = rateRelativeDateOption;
  }

  public ThreeFieldDuration getRelativeDuration() {
    return relativeDuration;
  }

  public void setRelativeDuration(ThreeFieldDuration relativeDuration) {
    this.relativeDuration = relativeDuration;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public BigDecimal getRentalRate() {
    return rentalRate;
  }

  public void setRentalRate(BigDecimal rentalRate) {
    this.rentalRate = rentalRate;
  }

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.RATE;
  }

  @Override
  public String getEventCaption() {
    if (rateDefinitionOption.equals(RateDefinitionOption.TERM)) {
      return "%s rate".formatted(licenceScheduleTerm.getTermType().getDisplayName());
    }
    if (rateDefinitionOption.equals(RateDefinitionOption.PHASE)) {
      return "%s rate".formatted(licenceSchedulePhase.getPhaseType().getDisplayName());
    }
    return "Rate";
  }
}

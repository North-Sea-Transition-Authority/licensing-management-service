package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cs_term_migration_extract")
@MigrationEntity
public class CarbonStorageTermMigrationExtract {

  @Id
  private Integer id;

  private String licenceRef;

  private String term;

  private Integer years;

  private Integer months;

  private Integer days;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getLicenceRef() {
    return licenceRef;
  }

  public void setLicenceRef(String licenceRef) {
    this.licenceRef = licenceRef;
  }

  public String getTerm() {
    return term;
  }

  public void setTerm(String term) {
    this.term = term;
  }

  public Integer getYears() {
    return years;
  }

  public void setYears(Integer years) {
    this.years = years;
  }

  public Integer getMonths() {
    return months;
  }

  public void setMonths(Integer months) {
    this.months = months;
  }

  public Integer getDays() {
    return days;
  }

  public void setDays(Integer days) {
    this.days = days;
  }
}

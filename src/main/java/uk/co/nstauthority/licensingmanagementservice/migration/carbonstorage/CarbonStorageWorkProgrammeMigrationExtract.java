package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;

@Entity
@Table(name = "cs_work_programme_migration_extract")
@MigrationEntity
public class CarbonStorageWorkProgrammeMigrationExtract {

  @Id
  private Integer id;

  private String licenceRef;

  private String caseId;

  private String caseDate;

  private UUID uniqueEventId;

  private String category;

  private String otherCategory;

  private String description;

  private String commitment;

  private String status;

  private String term;

  @Enumerated(EnumType.STRING)
  private WorkProgrammeActivityDateOption dateOption;

  private Integer relativeYears;

  private Integer relativeMonths;

  private Integer relativeDays;

  private String comments;

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

  public String getCaseId() {
    return caseId;
  }

  public void setCaseId(String caseId) {
    this.caseId = caseId;
  }

  public UUID getUniqueEventId() {
    return uniqueEventId;
  }

  public void setUniqueEventId(UUID uniqueEventId) {
    this.uniqueEventId = uniqueEventId;
  }

  public String getCaseDate() {
    return caseDate;
  }

  public void setCaseDate(String caseDate) {
    this.caseDate = caseDate;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getOtherCategory() {
    return otherCategory;
  }

  public void setOtherCategory(String otherCategory) {
    this.otherCategory = otherCategory;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCommitment() {
    return commitment;
  }

  public void setCommitment(String commitment) {
    this.commitment = commitment;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getTerm() {
    return term;
  }

  public void setTerm(String term) {
    this.term = term;
  }

  public WorkProgrammeActivityDateOption getDateOption() {
    return dateOption;
  }

  public void setDateOption(
      WorkProgrammeActivityDateOption dateOption) {
    this.dateOption = dateOption;
  }

  public Integer getRelativeYears() {
    return relativeYears;
  }

  public void setRelativeYears(Integer relativeYears) {
    this.relativeYears = relativeYears;
  }

  public Integer getRelativeMonths() {
    return relativeMonths;
  }

  public void setRelativeMonths(Integer relativeMonths) {
    this.relativeMonths = relativeMonths;
  }

  public Integer getRelativeDays() {
    return relativeDays;
  }

  public void setRelativeDays(Integer relativeDays) {
    this.relativeDays = relativeDays;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }
}

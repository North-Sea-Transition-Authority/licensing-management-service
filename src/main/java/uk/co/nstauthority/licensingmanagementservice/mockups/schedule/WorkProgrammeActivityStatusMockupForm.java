package uk.co.nstauthority.licensingmanagementservice.mockups.schedule;

import java.util.ArrayList;
import java.util.List;

/**
 * Mockup copy of {@code WorkProgrammeActivityStatusForm}. The transferred to licence field is replaced by the fields
 * backing the alternative work programme option: the add to list of licences and the explanatory comment.
 */
public class WorkProgrammeActivityStatusMockupForm {

  private MockWorkProgrammeStatus status;

  /**
   * Populated by the FDS add to list hidden input as a comma separated list of licence ids.
   */
  private List<String> alternativeWorkProgrammeLicenceIds = new ArrayList<>();

  /**
   * Backs the search selector the add to list adds rows from. Never submitted as part of the list itself.
   */
  private String alternativeWorkProgrammeLicenceSelector;

  private String alternativeWorkProgrammeComment;

  public MockWorkProgrammeStatus getStatus() {
    return status;
  }

  public void setStatus(MockWorkProgrammeStatus status) {
    this.status = status;
  }

  public List<String> getAlternativeWorkProgrammeLicenceIds() {
    return alternativeWorkProgrammeLicenceIds;
  }

  public void setAlternativeWorkProgrammeLicenceIds(List<String> alternativeWorkProgrammeLicenceIds) {
    this.alternativeWorkProgrammeLicenceIds = alternativeWorkProgrammeLicenceIds;
  }

  public String getAlternativeWorkProgrammeLicenceSelector() {
    return alternativeWorkProgrammeLicenceSelector;
  }

  public void setAlternativeWorkProgrammeLicenceSelector(String alternativeWorkProgrammeLicenceSelector) {
    this.alternativeWorkProgrammeLicenceSelector = alternativeWorkProgrammeLicenceSelector;
  }

  public String getAlternativeWorkProgrammeComment() {
    return alternativeWorkProgrammeComment;
  }

  public void setAlternativeWorkProgrammeComment(String alternativeWorkProgrammeComment) {
    this.alternativeWorkProgrammeComment = alternativeWorkProgrammeComment;
  }
}

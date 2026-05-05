package uk.co.nstauthority.licensingmanagementservice.mockups.decisionjourney.workprogrammeamendment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class WorkProgrammeAmendmentMockupForm {
  private Map<String, Boolean> selectedWorkProgrammes = new HashMap<>();
  private Map<String, String> workProgrammeActions = new HashMap<>();
  private Map<String, ThreeFieldDurationInput> workProgrammeDurations = new HashMap<>();
  private Map<String, String> workProgrammeAmendedTexts = new HashMap<>();
  private Map<String, Boolean> workProgrammeAmendOrExtend = new HashMap<>();
  private Map<String, List<String>> workProgrammeTargetLicences = new HashMap<>();
  private Map<String, String> workProgrammeTransferSelectors = new HashMap<>();

  public Map<String, Boolean> getSelectedWorkProgrammes() {
    return selectedWorkProgrammes;
  }

  public void setSelectedWorkProgrammes(Map<String, Boolean> selectedWorkProgrammes) {
    this.selectedWorkProgrammes = selectedWorkProgrammes;
  }

  public Map<String, String> getWorkProgrammeActions() {
    return workProgrammeActions;
  }

  public void setWorkProgrammeActions(Map<String, String> workProgrammeActions) {
    this.workProgrammeActions = workProgrammeActions;
  }

  public Map<String, ThreeFieldDurationInput> getWorkProgrammeDurations() {
    return workProgrammeDurations;
  }

  public void setWorkProgrammeDurations(Map<String, ThreeFieldDurationInput> workProgrammeDurations) {
    this.workProgrammeDurations = workProgrammeDurations;
  }

  public Map<String, String> getWorkProgrammeAmendedTexts() {
    return workProgrammeAmendedTexts;
  }

  public void setWorkProgrammeAmendedTexts(Map<String, String> workProgrammeAmendedTexts) {
    this.workProgrammeAmendedTexts = workProgrammeAmendedTexts;
  }

  public Map<String, Boolean> getWorkProgrammeAmendOrExtend() {
    return workProgrammeAmendOrExtend;
  }

  public void setWorkProgrammeAmendOrExtend(Map<String, Boolean> workProgrammeAmendOrExtend) {
    this.workProgrammeAmendOrExtend = workProgrammeAmendOrExtend;
  }

  public Map<String, List<String>> getWorkProgrammeTargetLicences() {
    return workProgrammeTargetLicences;
  }

  public void setWorkProgrammeTargetLicences(Map<String, List<String>> workProgrammeTargetLicences) {
    this.workProgrammeTargetLicences = workProgrammeTargetLicences;
  }

  public Map<String, String> getWorkProgrammeTransferSelectors() {
    return workProgrammeTransferSelectors;
  }

  public void setWorkProgrammeTransferSelectors(Map<String, String> workProgrammeTransferSelectors) {
    this.workProgrammeTransferSelectors = workProgrammeTransferSelectors;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.HashMap;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class RecordReductionDetailsForm {

  public static ThreeFieldDurationInput newDurationInput(String id) {
    return new ThreeFieldDurationInput("reductionDuration[" + id + "]", "reduction");
  }

  private Map<String, ThreeFieldDurationInput> reductionDuration = new HashMap<>();

  private Map<String, Boolean> selectedPhase = new HashMap<>();

  private Map<String, Boolean> selectedTerm = new HashMap<>();

  public Map<String, ThreeFieldDurationInput> getReductionDuration() {
    return reductionDuration;
  }

  public void setReductionDuration(Map<String, ThreeFieldDurationInput> reductionDuration) {
    this.reductionDuration = reductionDuration;
  }

  public Map<String, Boolean> getSelectedPhase() {
    return selectedPhase;
  }

  public void setSelectedPhase(Map<String, Boolean> selectedPhase) {
    this.selectedPhase = selectedPhase;
  }

  public Map<String, Boolean> getSelectedTerm() {
    return selectedTerm;
  }

  public void setSelectedTerm(Map<String, Boolean> selectedTerm) {
    this.selectedTerm = selectedTerm;
  }
}

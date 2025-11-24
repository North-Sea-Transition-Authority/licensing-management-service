package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import java.util.HashMap;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class LicenceScheduleExtensionForm {

  private Map<String, ThreeFieldDurationInput> extensionDuration = new HashMap<>();

  private Map<String, Boolean> selectedPhase = new HashMap<>();

  private Map<String, Boolean> selectedTerm = new HashMap<>();

  public Map<String, ThreeFieldDurationInput> getExtensionDuration() {
    return extensionDuration;
  }

  public void setExtensionDuration(Map<String, ThreeFieldDurationInput> extensionDuration) {
    this.extensionDuration = extensionDuration;
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
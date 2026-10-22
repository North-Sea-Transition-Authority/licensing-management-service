package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

// Session-persisted: mutate only via the controller's updateJourney() so changes are saved
class UploadDspMockupJourney implements Serializable {

  record DspVersion(int corrections, int addendums) implements Serializable {
  }

  record CaseEvent(String heading, String detailKey, String detailValue, Instant at) implements Serializable {
  }

  private int corrections;
  private int addendums;
  private final Set<Integer> correctedAddendums = new HashSet<>();
  private boolean sentBack;
  private DspVersion decisionSnapshot;
  private DspVersion decisionRecorded;
  private DecisionDetailsMockupForm decisionDetails;
  private final List<CaseEvent> events = new ArrayList<>();

  DspVersion dspVersion() {
    return new DspVersion(corrections, addendums);
  }

  boolean dspCorrected() {
    return corrections > 0;
  }

  int addendumCount() {
    return addendums;
  }

  String currentDspFileName() {
    return dspCorrected() ? "final-dsp-P1234-corrected.pdf" : "final-dsp-P1234.pdf";
  }

  List<String> addendumFileNames() {
    return IntStream.rangeClosed(1, addendums)
        .mapToObj(i -> "dsp-addendum-" + i + (correctedAddendums.contains(i) ? "-corrected" : "") + ".pdf")
        .toList();
  }

  boolean isValidAddendumNumber(int addendumNumber) {
    return addendumNumber >= 1 && addendumNumber <= addendums;
  }

  void uploadFirstDsp() {
    addEvent("DSP uploaded", currentDspFileName());
  }

  void correctFinalDsp() {
    corrections++;
    addEvent("DSP corrected", currentDspFileName());
  }

  void correctAddendum(int addendumNumber) {
    corrections++;
    correctedAddendums.add(addendumNumber);
    addEvent("DSP corrected", addendumFileNames().get(addendumNumber - 1));
  }

  void addAddendum() {
    addendums++;
    addEvent("DSP addendum uploaded", addendumFileNames().getLast());
  }

  private void addEvent(String heading, String fileName) {
    events.add(new CaseEvent(heading, "File", fileName, Instant.now()));
  }

  List<CaseEvent> eventsNewestFirst() {
    return events.reversed();
  }

  boolean isSentBack() {
    return sentBack;
  }

  void setSentBack(boolean sentBack) {
    this.sentBack = sentBack;
  }

  boolean isDecisionInProgress() {
    return decisionSnapshot != null;
  }

  // The snapshot is only taken once, so changes are measured from when the decision was first started
  void startDecision() {
    if (decisionSnapshot == null) {
      decisionSnapshot = dspVersion();
    }
  }

  boolean isDecisionRecorded() {
    return decisionRecorded != null;
  }

  void recordDecision() {
    decisionRecorded = dspVersion();
    decisionSnapshot = null;
    sentBack = false;
  }

  boolean hasDspChangedSinceDecisionRecorded() {
    return decisionRecorded != null && !decisionRecorded.equals(dspVersion());
  }

  List<String> changesSinceDecisionStarted() {
    var changes = new ArrayList<String>();
    if (decisionSnapshot == null) {
      return changes;
    }
    if (corrections > decisionSnapshot.corrections()) {
      changes.add("The DSP was replaced with a corrected version.");
    }
    var addendumsAdded = addendums - decisionSnapshot.addendums();
    if (addendumsAdded == 1) {
      changes.add("1 DSP addendum was added.");
    } else if (addendumsAdded > 1) {
      changes.add(addendumsAdded + " DSP addendums were added.");
    }
    return changes;
  }

  Optional<DecisionDetailsMockupForm> decisionDetails() {
    return Optional.ofNullable(decisionDetails);
  }

  void saveDecisionDetails(DecisionDetailsMockupForm decisionDetails) {
    this.decisionDetails = decisionDetails;
  }
}

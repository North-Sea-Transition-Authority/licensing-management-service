package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;

// Session-persisted: mutate only via the controller's updateJourney() so changes are saved
class UploadDspMockupJourney implements Serializable {

  private static final long serialVersionUID = 1L;

  record DspVersion(int corrections, int addendums) implements Serializable {
  }

  record CaseEvent(String heading, String detailKey, String detailValue, String eventBy, Instant at)
      implements Serializable {
  }

  static final String DECISION_LETTER_ISSUED = "Decision letter issued";
  static final String DECISION_ACCEPTED = "Decision accepted by industry";
  static final String DIRECTION_LETTER_ISSUED = "Direction letter issued";
  static final String DEED_SENT = "Deed of variation sent to licensees";

  private static final LocalDate DEFAULT_DECISION_DATE = LocalDate.of(2026, 4, 14);
  private static final String DECISION_ISSUER = "Riley Chen";
  private static final String CASE_MANAGER = "Jane Smith";
  private static final String DECISION_EXECUTOR = "Taylor Brooks";

  // Public so Freemarker can read it. Contact is null when the licensee has no contact for the licence
  public record Licensee(String name, String contact) {
  }

  static final List<Licensee> LICENSEES = List.of(
      new Licensee("Shell UK limited", "licensing@shell-uk.example.com"),
      new Licensee("BP Exploration Operating Company Limited", null)
  );

  enum SignatureStatus {
    NOT_REQUESTED,
    AWAITING,
    SENT_BACK,
    SIGNED
  }

  // Each licensee moves on their own: once their scan is accepted they post the hardcopies, without waiting for the others
  enum DeedStatus {
    NOT_STARTED,
    LICENSEE_SIGNING,
    NSTA_SIGNING,
    POSTING_BACK,
    COMPLETE
  }

  enum ScanStatus {
    AWAITING_SCAN,
    SCAN_RECEIVED,
    SENT_BACK,
    ACCEPTED
  }

  private DeedStatus deedStatus = DeedStatus.NOT_STARTED;
  private LocalDate decisionDate;
  private LocalDate signingDeadline;
  private final Map<Integer, ScanStatus> scanStatuses = new HashMap<>();
  private final Map<Integer, String> sendBackReasons = new HashMap<>();
  private final Set<Integer> hardcopiesReceived = new HashSet<>();
  private final Set<Integer> hardcopiesNotRequired = new HashSet<>();
  private String signatoryName;
  private String signatureFeedback;
  private SignatureStatus signatureStatus = SignatureStatus.NOT_REQUESTED;
  private boolean letterIssued;
  private boolean decisionLetterIssued;
  private boolean withDecisionIssuer;
  private boolean decisionAccepted;
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

  // The date entered with the first DSP. Blank when the user didn't enter one, as nothing is validated
  void uploadFirstDsp(LocalDate decisionDate) {
    this.decisionDate = decisionDate;
    addEvent("DSP uploaded", currentDspFileName());
  }

  // Null when not entered, or when a session saved before this field existed is loaded
  LocalDate decisionDate() {
    return decisionDate == null ? DEFAULT_DECISION_DATE : decisionDate;
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
    addEvent(heading, "File", fileName, CASE_MANAGER);
  }

  private void addEvent(String heading, String detailKey, String detailValue, String eventBy) {
    events.add(new CaseEvent(heading, detailKey, detailValue, eventBy, Instant.now()));
  }

  String recordedDocumentType() {
    return decisionDetails == null ? null : decisionDetails.getDocumentType();
  }

  SignatureStatus signatureStatus() {
    return signatureStatus;
  }

  String signatoryName() {
    return signatoryName;
  }

  boolean isLetterIssued() {
    return letterIssued;
  }

  String signatureFeedback() {
    return signatureFeedback;
  }

  void sendBackLetter(String feedback) {
    signatureFeedback = feedback;
    signatureStatus = SignatureStatus.SENT_BACK;
    addEvent("Direction letter sent back", "Feedback", feedback, signatoryName);
  }

  void sendForSignature(String signatoryName) {
    this.signatoryName = signatoryName;
    signatureFeedback = null;
    signatureStatus = SignatureStatus.AWAITING;
    addEvent("Direction letter sent for signature", "Authorised signatory", signatoryName, DECISION_EXECUTOR);
  }

  void signLetter() {
    signatureStatus = SignatureStatus.SIGNED;
    addEvent("Direction letter signed", "Authorised signatory", signatoryName, signatoryName);
  }

  void sendSignatureReminder() {
    addEvent("Signature reminder sent", "Authorised signatory", signatoryName, DECISION_EXECUTOR);
  }

  // Null when a session saved before the deed existed is loaded: field initialisers don't run on deserialisation
  DeedStatus deedStatus() {
    return deedStatus == null ? DeedStatus.NOT_STARTED : deedStatus;
  }

  LocalDate signingDeadline() {
    return signingDeadline;
  }

  void sendDeedToLicensees(LocalDate signingDeadline) {
    this.signingDeadline = signingDeadline;
    deedStatus = DeedStatus.LICENSEE_SIGNING;
    for (var i = 0; i < LICENSEES.size(); i++) {
      scanStatuses.put(i, ScanStatus.AWAITING_SCAN);
    }
    addEvent(DEED_SENT, "Signing deadline",
        DateFormatUtil.convertToDisplayText(signingDeadline), DECISION_EXECUTOR);
  }

  void issueDecisionLetter() {
    decisionLetterIssued = true;
    addEvent(DECISION_LETTER_ISSUED, "Sent", "By email from LMS", DECISION_ISSUER);
  }

  boolean isWithDecisionIssuer() {
    return withDecisionIssuer;
  }

  void passToDecisionIssuer() {
    withDecisionIssuer = true;
  }

  void returnToCaseManager() {
    withDecisionIssuer = false;
  }

  boolean isDecisionLetterIssued() {
    return decisionLetterIssued;
  }

  boolean hasDspUploaded() {
    return latestEvent("DSP uploaded").isPresent();
  }

  // The latest DSP upload: the first upload, a correction or an addendum
  Optional<Instant> dspUploadedAt() {
    return events.reversed().stream()
        .filter(event -> event.heading().startsWith("DSP "))
        .findFirst()
        .map(CaseEvent::at);
  }

  // The most recent one, as a letter reissued after a send back replaces the earlier letter
  Optional<CaseEvent> latestEvent(String heading) {
    return events.reversed().stream().filter(event -> heading.equals(event.heading())).findFirst();
  }

  ScanStatus scanStatus(int licensee) {
    return scanStatuses.getOrDefault(licensee, ScanStatus.AWAITING_SCAN);
  }

  String sendBackReason(int licensee) {
    return sendBackReasons.get(licensee);
  }

  boolean canUploadSignedDeed(int licensee) {
    var status = scanStatus(licensee);
    return deedStatus() == DeedStatus.LICENSEE_SIGNING
        && (status == ScanStatus.AWAITING_SCAN || status == ScanStatus.SENT_BACK);
  }

  boolean isHardcopyNotRequired(int licensee) {
    return hardcopiesNotRequired.contains(licensee);
  }

  List<Licensee> licenseesWithHardcopies() {
    return IntStream.range(0, LICENSEES.size())
        .filter(i -> !hardcopiesNotRequired.contains(i))
        .mapToObj(LICENSEES::get)
        .toList();
  }

  // Accepted, so the licensee has been asked to post their hardcopies and they haven't arrived yet
  boolean isAwaitingHardcopy(int licensee) {
    return deedStatus() == DeedStatus.LICENSEE_SIGNING
        && scanStatus(licensee) == ScanStatus.ACCEPTED
        && !hardcopiesReceived.contains(licensee);
  }

  // The licensee uploads their own scan. BP has no contact in the mockup, so they are named instead
  void receiveSignedDeed(int licensee) {
    markScanReceived(licensee);
    var signer = LICENSEES.get(licensee);
    addEvent("Signed deed of variation received", "Received from", signer.name(),
        signer.contact() == null ? signer.name() : signer.contact());
  }

  // A hardcopy posted at the soft copy stage, scanned and uploaded by the executor
  void receivePostedHardcopy(int licensee) {
    markScanReceived(licensee);
    addEvent("Posted hardcopy uploaded on behalf of licensee", "Licensee", LICENSEES.get(licensee).name(),
        DECISION_EXECUTOR);
  }

  private void markScanReceived(int licensee) {
    scanStatuses.put(licensee, ScanStatus.SCAN_RECEIVED);
    sendBackReasons.remove(licensee);
  }

  // An accepted posted hardcopy ticks the hardcopy stage as well, so the licensee has nothing to post later
  void acceptSignedDeed(int licensee, boolean postedHardcopy) {
    scanStatuses.put(licensee, ScanStatus.ACCEPTED);
    if (postedHardcopy) {
      hardcopiesReceived.add(licensee);
    }
    var name = LICENSEES.get(licensee).name();
    addEvent("Signed deed of variation accepted", "Licensee", name, DECISION_EXECUTOR);
    if (!postedHardcopy) {
      addEvent("Licensee asked to post hardcopies", "Licensee", name, "LMS");
    }
    moveOnWhenAllHardcopiesReceived();
  }

  private void moveOnWhenAllHardcopiesReceived() {
    if (hardcopiesReceived.size() == LICENSEES.size()) {
      deedStatus = DeedStatus.NSTA_SIGNING;
    }
  }

  // The scan is enough, for example when hardcopies could not be sent, so nothing is asked of the licensee
  void acceptWithoutHardcopy(int licensee) {
    scanStatuses.put(licensee, ScanStatus.ACCEPTED);
    hardcopiesReceived.add(licensee);
    hardcopiesNotRequired.add(licensee);
    var name = LICENSEES.get(licensee).name();
    addEvent("Signed deed of variation accepted", "Licensee", name, DECISION_EXECUTOR);
    addEvent("Hardcopies not required", "Licensee", name, DECISION_EXECUTOR);
    moveOnWhenAllHardcopiesReceived();
  }

  void sendBackSignedDeed(int licensee, String reason) {
    scanStatuses.put(licensee, ScanStatus.SENT_BACK);
    sendBackReasons.put(licensee, reason);
    addEvent("Signed deed of variation sent back to " + LICENSEES.get(licensee).name(), "Reason", reason,
        DECISION_EXECUTOR);
  }

  void receiveHardcopy(int licensee) {
    hardcopiesReceived.add(licensee);
    addEvent("Hardcopies received", "From", LICENSEES.get(licensee).name(), DECISION_EXECUTOR);
    moveOnWhenAllHardcopiesReceived();
  }

  // With no hardcopies to post back, the deed is complete once the conformed copy has been emailed
  void sendConformedDeed() {
    deedStatus = licenseesWithHardcopies().isEmpty() ? DeedStatus.COMPLETE : DeedStatus.POSTING_BACK;
    addEvent("Conformed deed of variation sent", "Scanned copy", "Emailed to the licensees", DECISION_EXECUTOR);
  }

  void completeDeed() {
    deedStatus = DeedStatus.COMPLETE;
    addEvent("Deed of variation completed", "Hardcopies", "Posted to the licensees", DECISION_EXECUTOR);
  }

  void issueLetter() {
    letterIssued = true;
    addEvent(DIRECTION_LETTER_ISSUED, "Sent", "By email from LMS", DECISION_EXECUTOR);
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
    withDecisionIssuer = true;
    decisionLetterIssued = false;
    decisionAccepted = false;
  }

  boolean isDecisionAccepted() {
    return decisionAccepted;
  }

  void acceptDecision() {
    decisionAccepted = true;
    var applicant = LICENSEES.getFirst().name();
    addEvent(DECISION_ACCEPTED, "Accepted by", applicant, applicant);
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

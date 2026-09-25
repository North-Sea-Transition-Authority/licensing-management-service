package uk.co.nstauthority.licensingmanagementservice.migration.pears.history;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a LICENCE_OPERATION_HISTORY document, so a test can say what PEARS holds in PEARS' own terms. Everything
 * built here goes back through {@link LicenceHistoryReader}, so a test also exercises the binding and mapping.
 */
public final class PearsHistoryTestUtil {

  public static final String ADMINISTRATOR_REFERENCE = "XPT/ADMIN";

  private PearsHistoryTestUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static Builder history(String licenceType, int licenceNo) {
    return new Builder(licenceType, licenceNo);
  }

  /**
   * A whole licence history, as its positions.
   */
  public static final class Builder {

    private final String licenceType;
    private final int licenceNo;
    private final List<PositionBuilder> positions = new ArrayList<>();
    private long nextOperationId = 1000;

    private Builder(String licenceType, int licenceNo) {
      this.licenceType = licenceType;
      this.licenceNo = licenceNo;
    }

    /**
     * A transaction executed against the licence, which is what a position is.
     */
    public PositionBuilder position(
        long transactionId,
        String regulatorReference,
        LocalDate positionDate,
        int positionSequence
    ) {
      var position = new PositionBuilder(this, transactionId, regulatorReference, positionDate, positionSequence);
      positions.add(position);
      return position;
    }

    public LicenceOperationHistory build() {
      try {
        return LicenceHistoryReader.read(new StringReader(toXml()));
      } catch (IOException e) {
        throw new UncheckedIOException("Could not read the history this test built", e);
      }
    }

    public String toXml() {
      var entries = positions.stream().map(PositionBuilder::toXml).reduce("", String::concat);

      return """
          <LICENCE_OPERATION_HISTORY licence_type="%s" licence_no="%d">%s\
          </LICENCE_OPERATION_HISTORY>"""
          .formatted(licenceType, licenceNo, entries);
    }

    private long nextOperationId() {
      return nextOperationId++;
    }
  }

  /**
   * One transaction's operations, in the order PEARS made them.
   */
  public static final class PositionBuilder {

    private final Builder history;
    private final long transactionId;
    private final String regulatorReference;
    private final LocalDate positionDate;
    private final int positionSequence;
    private final List<String> operations = new ArrayList<>();

    private PositionBuilder(
        Builder history,
        long transactionId,
        String regulatorReference,
        LocalDate positionDate,
        int positionSequence
    ) {
      this.history = history;
      this.transactionId = transactionId;
      this.regulatorReference = regulatorReference;
      this.positionDate = positionDate;
      this.positionSequence = positionSequence;
    }

    /**
     * A consortium list bringing the administrator into being, or naming a new one outright.
     */
    public PositionBuilder administratorSet(int organisationId) {
      return administratorList(PearsOperationType.CONSORTIUM_LIST_CHANGE, "LIVE", """
          <CONSORTIUM_ENTRY>
            <ENTRY_TYPE>SET</ENTRY_TYPE>
            <PRIMARY_ORGANISATION><NAME>Organisation %d</NAME><ID>%d</ID></PRIMARY_ORGANISATION>
          </CONSORTIUM_ENTRY>""".formatted(organisationId, organisationId));
    }

    /**
     * The administrator moving from one organisation to another, where the joining administrator
     * is the secondary side.
     */
    public PositionBuilder administratorTransfer(int fromOrganisationId, int toOrganisationId) {
      return administratorList(PearsOperationType.CONSORTIUM_LIST_CHANGE, "LIVE", """
          <CONSORTIUM_ENTRY>
            <ENTRY_TYPE>TRANSFER</ENTRY_TYPE>
            <PRIMARY_ORGANISATION><NAME>Organisation %d</NAME><ID>%d</ID></PRIMARY_ORGANISATION>
            <PRIMARY_STATUS>EXTANT</PRIMARY_STATUS>
            <SECONDARY_ORGANISATION><NAME>Organisation %d</NAME><ID>%d</ID></SECONDARY_ORGANISATION>
          </CONSORTIUM_ENTRY>"""
          .formatted(fromOrganisationId, fromOrganisationId, toOrganisationId, toOrganisationId));
    }

    public PositionBuilder administratorRemove(int organisationId) {
      return administratorList(PearsOperationType.CONSORTIUM_LIST_CHANGE, "LIVE", """
          <CONSORTIUM_ENTRY>
            <ENTRY_TYPE>REMOVE</ENTRY_TYPE>
            <PRIMARY_ORGANISATION><NAME>Organisation %d</NAME><ID>%d</ID></PRIMARY_ORGANISATION>
          </CONSORTIUM_ENTRY>""".formatted(organisationId, organisationId));
    }

    /**
     * An administrator entry PEARS gave no organisation, which the feed emits as an empty element.
     */
    public PositionBuilder administratorSetWithNoOrganisation() {
      return administratorList(PearsOperationType.CONSORTIUM_LIST_CHANGE, "LIVE", """
          <CONSORTIUM_ENTRY>
            <ENTRY_TYPE>SET</ENTRY_TYPE>
            <PRIMARY_ORGANISATION />
          </CONSORTIUM_ENTRY>""");
    }

    /**
     * A company list that is not the administrator, which arrives with its XML because the type is
     * fetched but is nothing this migration carries across.
     */
    public PositionBuilder companyList(String companyListType, int organisationId) {
      return consortiumOperation(PearsOperationType.CONSORTIUM_LIST_CHANGE, companyListType, "LIVE", """
          <CONSORTIUM_ENTRY>
            <ENTRY_TYPE>SET</ENTRY_TYPE>
            <PRIMARY_ORGANISATION><NAME>Organisation %d</NAME><ID>%d</ID></PRIMARY_ORGANISATION>
          </CONSORTIUM_ENTRY>""".formatted(organisationId, organisationId));
    }

    /**
     * An administrator change made inside a licence correction, which PEARS marks CORRECTED. The
     * status says how the operation came to exist, not that something later replaced it -- the
     * operation a correction replaces is tombstoned LEGACY-DELETED and never reaches this
     * application -- so this is live history like any other.
     */
    public PositionBuilder correctedAdministratorSet(int organisationId) {
      return administratorList(PearsOperationType.CONSORTIUM_LIST_CHANGE, "CORRECTED", """
          <CONSORTIUM_ENTRY>
            <ENTRY_TYPE>SET</ENTRY_TYPE>
            <PRIMARY_ORGANISATION><NAME>Organisation %d</NAME><ID>%d</ID></PRIMARY_ORGANISATION>
          </CONSORTIUM_ENTRY>""".formatted(organisationId, organisationId));
    }

    /**
     * An operation of a type no migrator asked for, so the query fetched no XML for it. This is
     * most of a real licence's history.
     */
    public PositionBuilder operationWithoutPayload(String operationType) {
      return operationWithoutPayload(operationType, "LIVE");
    }

    public PositionBuilder operationWithoutPayload(String operationType, String operationStatus) {
      operations.add(entry(operationType, operationStatus, ""));
      return this;
    }

    /**
     * The licence ending, which PEARS records as an operation like any other.
     */
    public PositionBuilder licenceEnd() {
      return operationWithoutPayload(PearsOperationType.LICENCE_END);
    }

    public Builder and() {
      return history;
    }

    public LicenceOperationHistory build() {
      return history.build();
    }

    public String toXml() {
      return String.join("", operations);
    }

    private PositionBuilder administratorList(String operationType, String operationStatus, String consortiumEntries) {
      return consortiumOperation(
          operationType, PearsCompanyListType.LICENCE_ADMINISTRATOR, operationStatus, consortiumEntries);
    }

    private PositionBuilder consortiumOperation(
        String operationType,
        String companyListType,
        String operationStatus,
        String consortiumEntries
    ) {
      operations.add(entry(operationType, operationStatus, """
          <OPERATION>
            <OPERATION_TYPE>%s</OPERATION_TYPE>
            <ATTRIBUTE_LIST><ATTRIBUTE_SET>
              <ATTRIBUTE><NAME>EVENT_DATE</NAME><VALUE>%s</VALUE></ATTRIBUTE>
              <ATTRIBUTE><NAME>COMPANY_LIST_TYPE</NAME><VALUE>%s</VALUE></ATTRIBUTE>
            </ATTRIBUTE_SET></ATTRIBUTE_LIST>
            <CONSORTIUM_ENTRY_LIST>%s</CONSORTIUM_ENTRY_LIST>
          </OPERATION>""".formatted(operationType, positionDate, companyListType, consortiumEntries)));
      return this;
    }

    private String entry(String operationType, String operationStatus, String payload) {
      return """
          <OPERATION_ENTRY tran_id="%d" regulator_reference="%s" position_date="%s" position_sequence="%d" \
          op_id="%d" op_seq="%d" op_status="%s" op_type="%s">%s</OPERATION_ENTRY>"""
          .formatted(
              transactionId,
              regulatorReference,
              positionDate,
              positionSequence,
              history.nextOperationId(),
              operations.size() + 1,
              operationStatus,
              operationType,
              payload);
    }
  }
}

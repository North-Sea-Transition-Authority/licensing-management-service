package uk.co.nstauthority.licensingmanagementservice.migration.pears.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PearsOperationMapperTest {

  private static final long TRAN_ID = 15188;
  private static final long OP_ID = 1000;
  private static final LocalDate POSITION_DATE = LocalDate.of(1964, Month.SEPTEMBER, 18);

  @Test
  void toOperation_whenABlockOperationAlsoCarriesAPayload_thenItIsStillReadAsABlockOperation() {
    var entry = entry(PearsOperationType.PED_BLOCK_CREATE, operation("EVENT_DATE", "1972-03-01"));

    var operation = PearsOperationMapper.toOperation(entry);

    assertThat(operation)
        .usingRecursiveComparison()
        .isEqualTo(new PearsOperation.BlockCreate(header(), List.of(), List.of()));
  }

  @Test
  void toOperation_whenAConsortiumListIsCreated_thenItIsReadWithItsEntries() {
    var entry = entry(
        PearsOperationType.CONSORTIUM_LIST_CREATE,
        operation(
            List.of(new LicenceHistoryXml.ConsortiumEntry(
                "SET",
                new LicenceHistoryXml.Organisation(12L, "Organisation 12"),
                "EXTANT",
                null
            )),
            "COMPANY_LIST_TYPE",
            PearsCompanyListType.LICENCE_ADMINISTRATOR
        )
    );

    var operation = PearsOperationMapper.toOperation(entry);

    assertThat(operation)
        .usingRecursiveComparison()
        .isEqualTo(new PearsOperation.ConsortiumListCreate(
            header(),
            null,
            PearsCompanyListType.LICENCE_ADMINISTRATOR,
            List.of(
                new PearsOperation.ConsortiumEntry(
                    PearsOperation.EntryType.SET,
                    new PearsOperation.Organisation(12, "Organisation 12"),
                    "EXTANT",
                    null
                )
            )
        ));
  }

  @Test
  void toOperation_whenTheTypeIsNotOneThisModelKnows_thenItIsUnrecognisedWithItsAttributes() {
    var entry = entry("PED_FIELD_CHANGE", operation("FIELD_ID", "7"));

    var operation = PearsOperationMapper.toOperation(entry);

    assertThat(operation)
        .usingRecursiveComparison()
        .isEqualTo(new PearsOperation.Unrecognised(
            header(),
            "PED_FIELD_CHANGE",
            Map.of("FIELD_ID", "7")
        ));
  }

  @Test
  void toOperation_whenAnAttributeHasNoNameOrNoValue_thenItIsAbsentRatherThanEmpty() {
    var entry = entry(
        "PED_FIELD_CHANGE",
        operation(
            "EMPTY_VALUE",
            null,
            null,
            "orphaned",
            "FIELD_ID",
            "7"
        )
    );

    var operation = PearsOperationMapper.toOperation(entry);

    assertThat(operation)
        .usingRecursiveComparison()
        .isEqualTo(new PearsOperation.Unrecognised(header(), "PED_FIELD_CHANGE", Map.of("FIELD_ID", "7")));
  }

  @ParameterizedTest
  @MethodSource("payloadsWithNoAttributes")
  void toOperation_whenThePayloadHoldsNoAttributes_thenNoneAreRead(LicenceHistoryXml.Operation payload) {
    var entry = entry("PED_FIELD_CHANGE", payload);

    var operation = PearsOperationMapper.toOperation(entry);

    assertThat(operation)
        .usingRecursiveComparison()
        .isEqualTo(new PearsOperation.Unrecognised(header(), "PED_FIELD_CHANGE", Map.of()));
  }

  private static Stream<LicenceHistoryXml.Operation> payloadsWithNoAttributes() {
    return Stream.of(
        new LicenceHistoryXml.Operation(null, null, null),
        new LicenceHistoryXml.Operation(List.of(new LicenceHistoryXml.AttributeSet(null, null)), null, null)
    );
  }

  @Test
  void toOperation_whenTheEventDateIsATimestamp_thenOnlyTheDateIsRead() {
    var entry = entry("PED_FIELD_CHANGE", operation("EVENT_DATE", "1972-03-01T09:30:00"));

    var operation = PearsOperationMapper.toOperation(entry);

    assertThat(operation.header().eventDate()).isEqualTo(LocalDate.of(1972, Month.MARCH, 1));
  }

  @Test
  void toOperation_whenTheOperationStatusIsMissing_thenTheReadFails() {
    var entry = new LicenceHistoryXml.Entry(
        TRAN_ID,
        "XPT/1",
        "1964-09-18",
        6,
        OP_ID,
        1,
        null,
        "PED_FIELD_CHANGE",
        operation(),
        null,
        null
    );

    assertThatThrownBy(() -> PearsOperationMapper.toOperation(entry))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Missing required attribute op_status");
  }

  @Test
  void toOperation_whenABlockEntryTypeIsNotOneThisModelKnows_thenItIsReadAsAbsentRatherThanFailing() {
    var entry = entry(
        PearsOperationType.PED_BLOCK_CHANGE,
        null,
        List.of(blockEntry("SPLIT", 11, "1", 22, "2"))
    );

    var operation = PearsOperationMapper.toOperation(entry);

    assertThat(((PearsOperation.BlockChange) operation).entries())
        .singleElement()
        .extracting(PearsOperation.BlockEntry::type)
        .isNull();
  }

  @Test
  void toOperation_whenABlockSideNamesNoIdentity_thenThatSideIsAbsent() {
    var entry = entry(
        PearsOperationType.PED_BLOCK_CREATE,
        null,
        List.of(new LicenceHistoryXml.BlockEntry(
            "SET",
            null,
            null,
            null,
            "a",
            "30/1a",
            null,
            new BigDecimal("2.5"),
            null,
            null,
            null,
            null,
            null,
            null
        ))
    );

    var operation = PearsOperationMapper.toOperation(entry);

    assertThat(((PearsOperation.BlockCreate) operation).entries())
        .usingRecursiveComparison()
        .isEqualTo(List.of(new PearsOperation.BlockEntry(PearsOperation.EntryType.SET, null, null, null)));
  }

  @Test
  void toOperation_whenABlockOperationNamesNoBlocks_thenItHoldsNoEntries() {
    var entry = entry(PearsOperationType.PED_BLOCK_END, null, null);

    var operation = PearsOperationMapper.toOperation(entry);

    assertThat(((PearsOperation.BlockEnd) operation).entries()).isEmpty();
  }

  static Stream<Arguments> spatialOperations() {
    return Stream.of(
        arguments(PearsOperationType.PED_BLOCK_CREATE, new PearsOperation.BlockCreate(header(), List.of(), List.of())),
        arguments(PearsOperationType.PED_BLOCK_CHANGE, new PearsOperation.BlockChange(header(), List.of(), List.of())),
        arguments(PearsOperationType.PED_BLOCK_END, new PearsOperation.BlockEnd(header(), List.of(), List.of())),
        arguments(PearsOperationType.PED_SUBAREA_CREATE, new PearsOperation.SubareaCreate(header(), List.of())),
        arguments(PearsOperationType.PED_SUBAREA_CHANGE, new PearsOperation.SubareaChange(header(), List.of())),
        arguments(PearsOperationType.PED_SUBAREA_END, new PearsOperation.SubareaEnd(header(), List.of()))
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("spatialOperations")
  void toOperation_whenTheTypeIsSpatial_thenItIsReadAsItsSpatialOperation(
      String opType,
      PearsOperation expected
  ) {
    var entry = entry(opType, null);

    var operation = PearsOperationMapper.toOperation(entry);

    assertThat(operation)
        .usingRecursiveComparison()
        .isEqualTo(expected);
  }

  @Test
  void toOperation_whenASubareaEntryNamesBothSides_thenBothAreRead() {
    var entry = entry(
        PearsOperationType.PED_SUBAREA_CHANGE,
        null,
        null,
        List.of(new LicenceHistoryXml.SubareaEntry(
            "TRANSFER",
            2,
            "NORTH",
            "DISCOVERY AREA",
            739277,
            "47/10a",
            739120,
            "DISCOVERY AREA",
            739026,
            "47/10",
            739051
        ))
    );

    var operation = PearsOperationMapper.toOperation(entry);

    assertThat(operation)
        .usingRecursiveComparison()
        .isEqualTo(new PearsOperation.SubareaChange(
            header(),
            List.of(new PearsOperation.SubareaEntry(
                PearsOperation.EntryType.TRANSFER,
                2,
                "NORTH",
                new PearsOperation.Subarea(739026, "DISCOVERY AREA", 739051, "47/10"),
                new PearsOperation.Subarea(739277, "DISCOVERY AREA", 739120, "47/10a")
            ))
        ));
  }

  @Test
  void toOperation_whenASubareaSideNamesNoSubareaOrBlock_thenThatSideIsAbsent() {
    var entry = entry(
        PearsOperationType.PED_SUBAREA_CREATE,
        null,
        null,
        List.of(new LicenceHistoryXml.SubareaEntry(
            "SET",
            null,
            "A",
            "ALL",
            739277,
            "47/10a",
            739120,
            "ALL",
            null,
            "47/10",
            null
        ))
    );

    var operation = PearsOperationMapper.toOperation(entry);

    assertThat(operation)
        .usingRecursiveComparison()
        .isEqualTo(new PearsOperation.SubareaCreate(
            header(),
            List.of(new PearsOperation.SubareaEntry(
                PearsOperation.EntryType.SET,
                null,
                "A",
                null,
                new PearsOperation.Subarea(739277, "ALL", 739120, "47/10a")
            ))
        ));
  }

  private static LicenceHistoryXml.Entry entry(
      String opType,
      LicenceHistoryXml.Operation operation
  ) {
    return entry(opType, operation, null);
  }

  private static LicenceHistoryXml.Entry entry(
      String opType,
      LicenceHistoryXml.Operation operation,
      List<LicenceHistoryXml.BlockEntry> blockEntries
  ) {
    return entry(opType, operation, blockEntries, null);
  }

  private static LicenceHistoryXml.Entry entry(
      String opType,
      LicenceHistoryXml.Operation operation,
      List<LicenceHistoryXml.BlockEntry> blockEntries,
      List<LicenceHistoryXml.SubareaEntry> subareaEntries
  ) {
    return new LicenceHistoryXml.Entry(
        TRAN_ID,
        "XPT/1",
        "1964-09-18",
        6,
        OP_ID,
        1,
        "LIVE",
        opType,
        operation,
        blockEntries,
        subareaEntries
    );
  }

  private static LicenceHistoryXml.Operation operation(String... attributePairs) {
    return operation(null, attributePairs);
  }

  private static LicenceHistoryXml.Operation operation(
      List<LicenceHistoryXml.ConsortiumEntry> consortiumEntries,
      String... attributePairs
  ) {
    var attributes = new ArrayList<LicenceHistoryXml.Attribute>();
    for (var i = 0; i < attributePairs.length; i += 2) {
      attributes.add(new LicenceHistoryXml.Attribute(attributePairs[i], attributePairs[i + 1]));
    }
    return new LicenceHistoryXml.Operation(
        List.of(new LicenceHistoryXml.AttributeSet(null, attributes)),
        null,
        consortiumEntries
    );
  }

  private static LicenceHistoryXml.BlockEntry blockEntry(
      String entryType,
      Integer inputSiId,
      String inputBlockNo,
      Integer outputSiId,
      String outputBlockNo
  ) {
    return new LicenceHistoryXml.BlockEntry(
        entryType,
        null,
        null,
        outputBlockNo,
        null,
        null,
        outputSiId,
        null,
        null,
        inputBlockNo,
        null,
        null,
        inputSiId,
        null
    );
  }

  private static PearsOperation.Header header() {
    return new PearsOperation.Header(
        TRAN_ID,
        "XPT/1",
        POSITION_DATE,
        6,
        OP_ID,
        1,
        PearsOperation.OperationStatus.LIVE,
        null,
        null
    );
  }
}

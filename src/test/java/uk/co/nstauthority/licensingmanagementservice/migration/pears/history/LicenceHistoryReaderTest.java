package uk.co.nstauthority.licensingmanagementservice.migration.pears.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LicenceHistoryReaderTest {

  private static final LocalDate POSITION_DATE = LocalDate.of(1964, 9, 18);

  @Test
  void read_whenAdministratorIsSet_thenTheConsortiumListIsRead() throws Exception {
    var xml = PearsHistoryTestUtil.history("P", 8)
        .position(15188, "XPT/1", POSITION_DATE, 6)
        .administratorSet(12)
        .and()
        .toXml();

    var history = LicenceHistoryReader.read(new StringReader(xml));

    var expected = new PearsOperation.ConsortiumListChange(
        new PearsOperation.Header(15188, "XPT/1", POSITION_DATE, 6, 1000, 1, PearsOperation.OperationStatus.LIVE, POSITION_DATE, null),
        null,
        PearsCompanyListType.LICENCE_ADMINISTRATOR,
        List.of(new PearsOperation.ConsortiumEntry(
            PearsOperation.EntryType.SET,
            new PearsOperation.Organisation(12, "Organisation 12"),
            null,
            null
        ))
    );

    assertThat(history.operations()).usingRecursiveComparison().isEqualTo(java.util.List.of(expected));
  }

  @Test
  void read_whenAnAdministratorIsTransferred_thenTheJoiningOrganisationIsTheSecondary() throws Exception {
    var xml = PearsHistoryTestUtil.history("P", 8)
        .position(15188, "XPT/1", POSITION_DATE, 6)
        .administratorTransfer(12, 34)
        .and()
        .toXml();

    var history = LicenceHistoryReader.read(new StringReader(xml));

    assertThat(history.operations())
        .singleElement()
        .isInstanceOfSatisfying(PearsOperation.ConsortiumListChange.class, change ->
            assertThat(change.entries())
                .singleElement()
                .usingRecursiveComparison()
                .isEqualTo(new PearsOperation.ConsortiumEntry(
                    PearsOperation.EntryType.TRANSFER,
                    new PearsOperation.Organisation(12, "Organisation 12"),
                    "EXTANT",
                    new PearsOperation.Organisation(34, "Organisation 34")
                ))
        );
  }

  @Test
  void read_whenAnEntryHasNoOrganisation_thenTheEmptyElementIsReadAsAbsent() throws Exception {
    var xml = PearsHistoryTestUtil.history("P", 8)
        .position(15188, "XPT/1", POSITION_DATE, 6)
        .administratorSetWithNoOrganisation()
        .and()
        .toXml();

    var history = LicenceHistoryReader.read(new StringReader(xml));

    assertThat(history.operations())
        .singleElement()
        .isInstanceOfSatisfying(PearsOperation.ConsortiumListChange.class, change ->
            assertThat(change.entries()).singleElement().extracting(PearsOperation.ConsortiumEntry::primary).isNull());
  }

  @Test
  void read_whenAnOperationHasNoPayload_thenItIsReadAsOmitted() throws Exception {
    var xml = PearsHistoryTestUtil.history("P", 8)
        .position(15188, "XPT/1", POSITION_DATE, 6)
        .operationWithoutPayload("PED_RETENTION_AREA_CHANGE")
        .and()
        .toXml();

    var history = LicenceHistoryReader.read(new StringReader(xml));

    var expected = new PearsOperation.Omitted(
        new PearsOperation.Header(
            15188, "XPT/1", POSITION_DATE, 6, 1000, 1, PearsOperation.OperationStatus.LIVE, null, null),
        "PED_RETENTION_AREA_CHANGE"
    );

    assertThat(history.operations()).usingRecursiveComparison().isEqualTo(java.util.List.of(expected));
  }

  @Test
  void read_whenTheLicenceEnds_thenTheEndOperationIsRead() throws Exception {
    var xml = PearsHistoryTestUtil.history("P", 8)
        .position(15188, "XPT/1", POSITION_DATE, 6)
        .licenceEnd()
        .and()
        .toXml();

    var history = LicenceHistoryReader.read(new StringReader(xml));

    assertThat(history.operations())
        .singleElement()
        .extracting(PearsOperation::typeName)
        .isEqualTo(PearsOperationType.LICENCE_END);
  }

  @Test
  void read_whenTheDocumentHoldsGeometry_thenItIsSkippedRatherThanFailingTheRead() throws Exception {
    var xml = """
        <LICENCE_OPERATION_HISTORY licence_type="P" licence_no="8">
          <OPERATION_ENTRY tran_id="15188" regulator_reference="XPT/1" position_date="1964-09-18"
                           position_sequence="6" op_id="1000" op_seq="1" op_status="LIVE"
                           op_type="PED_BLOCK_CREATE">
            <OPERATION>
              <OPERATION_TYPE>PED_BLOCK_CREATE</OPERATION_TYPE>
              <BLOCK_ENTRY_LIST><BLOCK_ENTRY>
                <ENTRY_TYPE>SET</ENTRY_TYPE>
                <PRIMARY_BLOCK><QUADRANT_NO>48</QUADRANT_NO><BLOCK_NO>13</BLOCK_NO>
                  <SPATIAL_FEATURE_SET><SPATIAL_FEATURE><AREA><BOUNDARY><CONNECTION><NODE>
                    <COORD><LAT_D>57</LAT_D><LON_D>1</LON_D></COORD>
                  </NODE></CONNECTION></BOUNDARY></AREA></SPATIAL_FEATURE></SPATIAL_FEATURE_SET>
                </PRIMARY_BLOCK>
              </BLOCK_ENTRY></BLOCK_ENTRY_LIST>
            </OPERATION>
          </OPERATION_ENTRY>
        </LICENCE_OPERATION_HISTORY>""";

    var history = LicenceHistoryReader.read(new StringReader(xml));

    // The blocks a block operation acted on reach the reader as a sibling of OPERATION, shredded out
    // by the query. The geometry inside the payload is not bound at all, which is the point here.
    assertThat(history.operations())
        .singleElement()
        .isInstanceOfSatisfying(PearsOperation.BlockCreate.class, create ->
            assertThat(create.entries()).isEmpty());
  }

  @Test
  void read_whenABlockOperationHoldsBlockEntries_thenBothSidesAreRead() throws Exception {
    var xml = """
        <LICENCE_OPERATION_HISTORY licence_type="P" licence_no="8" operation_count="1">
          <OPERATION_ENTRY tran_id="15188" regulator_reference="XPT/1" position_date="1964-09-18"
                           position_sequence="6" op_id="1000" op_seq="1" op_status="LIVE"
                           op_type="PED_BLOCK_CHANGE">
            <BLOCK_ENTRY_LIST>
              <BLOCK_ENTRY entry_type="TRANSFER"
                           output_quadrant_no="47" output_block_no="10" output_block_suffix="a"
                           output_block_ref="47/10a" output_si_id="739120" output_area_km2="117.4"
                           input_quadrant_no="47" input_block_no="10"
                           input_block_ref="47/10" input_si_id="739051" input_area_km2="184.1"/>
            </BLOCK_ENTRY_LIST>
          </OPERATION_ENTRY>
        </LICENCE_OPERATION_HISTORY>""";

    var history = LicenceHistoryReader.read(new StringReader(xml));

    var expected = new PearsOperation.BlockChange(
        new PearsOperation.Header(
            15188,
            "XPT/1",
            POSITION_DATE,
            6,
            1000,
            1,
            PearsOperation.OperationStatus.LIVE,
            null,
            null
        ),
        java.util.List.of(new PearsOperation.BlockEntry(
            PearsOperation.EntryType.TRANSFER,
            null,
            new PearsOperation.Block(739051, "47/10", "47", "10", null, new BigDecimal("184.1")),
            new PearsOperation.Block(739120, "47/10a", "47", "10", "a", new BigDecimal("117.4"))
        )),
        java.util.List.of()
    );

    assertThat(history.operations()).usingRecursiveComparison().isEqualTo(java.util.List.of(expected));
  }

  @Test
  void read_whenASubareaOperationHoldsSubareaEntries_thenEveryEntryTypeIsRead() throws Exception {
    var xml = """
        <LICENCE_OPERATION_HISTORY licence_type="P" licence_no="8" operation_count="1">
          <OPERATION_ENTRY tran_id="15188" regulator_reference="XPT/1" position_date="1964-09-18"
                           position_sequence="6" op_id="1000" op_seq="1" op_status="LIVE"
                           op_type="PED_SUBAREA_CHANGE">
            <SUBAREA_ENTRY_LIST>
              <SUBAREA_ENTRY entry_type="SET" subarea_short_name="SOUTH"
                             output_subarea_name="APPRAISAL AREA" output_subarea_si_id="739301"
                             output_block_ref="47/10a" output_block_si_id="739120"/>
              <SUBAREA_ENTRY entry_type="TRANSFER" block_entry_seq="2" subarea_short_name="NORTH"
                             output_subarea_name="DISCOVERY AREA" output_subarea_si_id="739277"
                             output_block_ref="47/10a" output_block_si_id="739120"
                             input_subarea_name="DISCOVERY AREA" input_subarea_si_id="739026"
                             input_block_ref="47/10" input_block_si_id="739051"/>
              <SUBAREA_ENTRY entry_type="REMOVE" block_entry_seq="3" subarea_short_name="EAST"
                             input_subarea_name="RELINQUISHED AREA" input_subarea_si_id="739030"
                             input_block_ref="47/10" input_block_si_id="739051"/>
            </SUBAREA_ENTRY_LIST>
          </OPERATION_ENTRY>
        </LICENCE_OPERATION_HISTORY>""";

    var history = LicenceHistoryReader.read(new StringReader(xml));

    var expected = new PearsOperation.SubareaChange(
        new PearsOperation.Header(
            15188,
            "XPT/1",
            POSITION_DATE,
            6,
            1000,
            1,
            PearsOperation.OperationStatus.LIVE,
            null,
            null
        ),
        List.of(
            new PearsOperation.SubareaEntry(
                PearsOperation.EntryType.SET,
                null,
                "SOUTH",
                null,
                new PearsOperation.Subarea(739301, "APPRAISAL AREA", 739120, "47/10a")
            ),
            new PearsOperation.SubareaEntry(
                PearsOperation.EntryType.TRANSFER,
                2,
                "NORTH",
                new PearsOperation.Subarea(739026, "DISCOVERY AREA", 739051, "47/10"),
                new PearsOperation.Subarea(739277, "DISCOVERY AREA", 739120, "47/10a")
            ),
            new PearsOperation.SubareaEntry(
                PearsOperation.EntryType.REMOVE,
                3,
                "EAST",
                new PearsOperation.Subarea(739030, "RELINQUISHED AREA", 739051, "47/10"),
                null
            )
        )
    );

    assertThat(history.operations()).usingRecursiveComparison().isEqualTo(List.of(expected));
  }

  @Test
  void read_whenTheDocumentIsEmpty_thenTheLicenceIsReadWithNoOperations() throws Exception {
    var xml = "<LICENCE_OPERATION_HISTORY licence_type=\"P\" licence_no=\"8\"/>";

    var history = LicenceHistoryReader.read(new StringReader(xml));

    assertThat(history).usingRecursiveComparison()
        .isEqualTo(new LicenceOperationHistory("P", 8, java.util.List.of()));
  }

  @Test
  void read_whenAnOperationStatusIsNotOneTheQueryReturns_thenTheReadFails() {
    var xml = """
        <LICENCE_OPERATION_HISTORY licence_type="P" licence_no="8">
          <OPERATION_ENTRY tran_id="1" regulator_reference="XPT/1" position_date="1964-09-18"
                           position_sequence="1" op_id="1" op_seq="1" op_status="WITHDRAWN"
                           op_type="LICENCE_END"/>
        </LICENCE_OPERATION_HISTORY>""";

    assertThatThrownBy(() -> LicenceHistoryReader.read(new StringReader(xml)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("WITHDRAWN");
  }

  @ParameterizedTest
  @ValueSource(strings = {"licence_no", "position_sequence", "op_seq", "op_type"})
  void read_whenARequiredAttributeIsMissing_thenTheReadFails(String attribute) {
    var xml = """
        <LICENCE_OPERATION_HISTORY licence_type="P" licence_no="8">
          <OPERATION_ENTRY tran_id="1" regulator_reference="XPT/1" position_date="1964-09-18"
                           position_sequence="1" op_id="1" op_seq="1" op_status="LIVE"
                           op_type="LICENCE_END"/>
        </LICENCE_OPERATION_HISTORY>"""
        .replaceFirst(attribute + "=\"[^\"]+\"", "");

    assertThatThrownBy(() -> LicenceHistoryReader.read(new StringReader(xml)))
        .isInstanceOf(MismatchedInputException.class)
        .hasMessageContaining(attribute);
  }
}

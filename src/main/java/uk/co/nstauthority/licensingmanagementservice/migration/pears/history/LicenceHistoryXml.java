package uk.co.nstauthority.licensingmanagementservice.migration.pears.history;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.math.BigDecimal;
import java.util.List;

/**
 * The verbatim shape of LICENCE_OPERATION_HISTORY, carrying no meaning and no validation beyond
 * being trivially bindable by Jackson. {@link PearsOperationMapper} turns it into the sealed
 * {@link PearsOperation} hierarchy, and null-checks the lists Jackson leaves null where the
 * document has no such element.
 */
interface LicenceHistoryXml {

  @JacksonXmlRootElement(localName = "LICENCE_OPERATION_HISTORY")
  record History(
      @JacksonXmlProperty(isAttribute = true, localName = "licence_type") String licenceType,
      @JsonProperty(required = true)
      @JacksonXmlProperty(isAttribute = true, localName = "licence_no") int licenceNo,
      @JacksonXmlProperty(localName = "OPERATION_ENTRY") List<Entry> entries
  ) {
  }

  /**
   * One operation, described by the transaction and the position it belongs to.
   *
   * @param tranId           required, so a document missing it fails the read rather than binding to zero
   * @param positionSequence required, on the same terms as {@code tranId}, since positions are ordered by it
   * @param opId             required, on the same terms as {@code tranId}
   * @param opSeq            required, on the same terms as {@code tranId}, since operations are ordered by it
   * @param opType           required; the operation's type, carried as an attribute so an operation
   *                         whose payload was not fetched is still known by type
   * @param operation        null where {@code licence-history.sql} did not fetch this type's XML
   * @param blockEntries     the blocks a block operation acted on. Populated instead of, not as well
   *                         as, {@code operation}: the query emits this section for block operations
   *                         and still withholds their payload, because the geometry inside it is the
   *                         bulk of a document and the identity here is all the migration reads.
   * @param subareaEntries   the subareas the operation acted on, on the same terms as
   *                         {@code blockEntries}. Present on a subarea operation and on a block
   *                         operation alike, since a block operation makes and ends subareas without
   *                         ever naming them in its own payload.
   */
  record Entry(
      @JsonProperty(required = true)
      @JacksonXmlProperty(isAttribute = true, localName = "tran_id") long tranId,
      @JacksonXmlProperty(isAttribute = true, localName = "regulator_reference") String regulatorReference,
      @JacksonXmlProperty(isAttribute = true, localName = "position_date") String positionDate,
      @JsonProperty(required = true)
      @JacksonXmlProperty(isAttribute = true, localName = "position_sequence") int positionSequence,
      @JsonProperty(required = true)
      @JacksonXmlProperty(isAttribute = true, localName = "op_id") long opId,
      @JsonProperty(required = true)
      @JacksonXmlProperty(isAttribute = true, localName = "op_seq") int opSeq,
      @JacksonXmlProperty(isAttribute = true, localName = "op_status") String opStatus,
      @JsonProperty(required = true)
      @JacksonXmlProperty(isAttribute = true, localName = "op_type") String opType,
      @JacksonXmlProperty(localName = "OPERATION") Operation operation,

      @JacksonXmlElementWrapper(localName = "BLOCK_ENTRY_LIST")
      @JacksonXmlProperty(localName = "BLOCK_ENTRY_LIST") List<BlockEntry> blockEntries,

      @JacksonXmlElementWrapper(localName = "SUBAREA_ENTRY_LIST")
      @JacksonXmlProperty(localName = "SUBAREA_ENTRY_LIST") List<SubareaEntry> subareaEntries
  ) {
  }

  /**
   * The before and after of a single subarea on a block operation.
   * Identity of the subarea is both the si_id and the name, as not all legacy subareas have a si_id.
   */
  record SubareaEntry(
      @JacksonXmlProperty(isAttribute = true, localName = "entry_type") String entryType,
      @JacksonXmlProperty(isAttribute = true, localName = "block_entry_seq") Integer blockEntrySeq,
      @JacksonXmlProperty(isAttribute = true, localName = "subarea_short_name") String subareaShortName,
      @JacksonXmlProperty(isAttribute = true, localName = "output_subarea_name") String outputSubareaName,
      @JacksonXmlProperty(isAttribute = true, localName = "output_subarea_si_id") Integer outputSubareaSiId,
      @JacksonXmlProperty(isAttribute = true, localName = "output_block_ref") String outputBlockRef,
      @JacksonXmlProperty(isAttribute = true, localName = "output_block_si_id") Integer outputBlockSiId,
      @JacksonXmlProperty(isAttribute = true, localName = "input_subarea_name") String inputSubareaName,
      @JacksonXmlProperty(isAttribute = true, localName = "input_subarea_si_id") Integer inputSubareaSiId,
      @JacksonXmlProperty(isAttribute = true, localName = "input_block_ref") String inputBlockRef,
      @JacksonXmlProperty(isAttribute = true, localName = "input_block_si_id") Integer inputBlockSiId
  ) {
  }

  /**
   * The before and after of a single block on a block operation.
   *
   */
  record BlockEntry(
      @JacksonXmlProperty(isAttribute = true, localName = "entry_type") String entryType,
      @JacksonXmlProperty(isAttribute = true, localName = "entry_seq") Integer entrySeq,
      @JacksonXmlProperty(isAttribute = true, localName = "output_quadrant_no") String outputQuadrantNo,
      @JacksonXmlProperty(isAttribute = true, localName = "output_block_no") String outputBlockNo,
      @JacksonXmlProperty(isAttribute = true, localName = "output_block_suffix") String outputBlockSuffix,
      @JacksonXmlProperty(isAttribute = true, localName = "output_block_ref") String outputBlockRef,
      @JacksonXmlProperty(isAttribute = true, localName = "output_si_id") Integer outputSiId,
      @JacksonXmlProperty(isAttribute = true, localName = "output_area_km2") BigDecimal outputAreaKm2,
      @JacksonXmlProperty(isAttribute = true, localName = "input_quadrant_no") String inputQuadrantNo,
      @JacksonXmlProperty(isAttribute = true, localName = "input_block_no") String inputBlockNo,
      @JacksonXmlProperty(isAttribute = true, localName = "input_block_suffix") String inputBlockSuffix,
      @JacksonXmlProperty(isAttribute = true, localName = "input_block_ref") String inputBlockRef,
      @JacksonXmlProperty(isAttribute = true, localName = "input_si_id") Integer inputSiId,
      @JacksonXmlProperty(isAttribute = true, localName = "input_area_km2") BigDecimal inputAreaKm2
  ) {
  }

  /**
   * The children of OPERATION this migration reads.
   */
  record Operation(
      @JacksonXmlElementWrapper(localName = "ATTRIBUTE_LIST")
      @JacksonXmlProperty(localName = "ATTRIBUTE_LIST") List<AttributeSet> attributeSets,

      @JacksonXmlProperty(localName = "SYSTEM_COMMENT") String systemComment,

      @JacksonXmlElementWrapper(localName = "CONSORTIUM_ENTRY_LIST")
      @JacksonXmlProperty(localName = "CONSORTIUM_ENTRY_LIST") List<ConsortiumEntry> consortiumEntries
  ) {
  }

  /**
   * {@code set_id} is absent on the OPERATION-level set and present on the nested spatial sets,
   * which this does not bind.
   */
  record AttributeSet(
      @JacksonXmlProperty(isAttribute = true, localName = "set_id") String setId,
      @JacksonXmlProperty(localName = "ATTRIBUTE") List<Attribute> attributes
  ) {
  }

  record Attribute(
      @JacksonXmlProperty(localName = "NAME") String name,
      @JacksonXmlProperty(localName = "VALUE") String value
  ) {
  }

  record ConsortiumEntry(
      @JacksonXmlProperty(localName = "ENTRY_TYPE") String entryType,
      @JacksonXmlProperty(localName = "PRIMARY_ORGANISATION") Organisation primaryOrganisation,
      @JacksonXmlProperty(localName = "PRIMARY_STATUS") String primaryStatus,
      @JacksonXmlProperty(localName = "SECONDARY_ORGANISATION") Organisation secondaryOrganisation
  ) {
  }

  record Organisation(
      @JacksonXmlProperty(localName = "ID") Long id,
      @JacksonXmlProperty(localName = "NAME") String name
  ) {
  }
}

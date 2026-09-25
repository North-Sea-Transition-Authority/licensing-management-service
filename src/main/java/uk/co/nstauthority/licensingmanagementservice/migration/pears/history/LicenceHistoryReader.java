package uk.co.nstauthority.licensingmanagementservice.migration.pears.history;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.io.Reader;

/**
 * Reads a LICENCE_OPERATION_HISTORY document.
 */
public final class LicenceHistoryReader {

  private static final XmlMapper MAPPER = XmlMapper.builder()
      // This document mixes wrapped and unwrapped repeating elements, so wrapping is declared
      // per property on the binding records instead.
      .defaultUseWrapper(false)
      // The document holds a great deal this migration does not read -- block, subarea and
      // geometry elements. Skipping them is the point rather than an oversight.
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      // Empty elements (<VALUE/>, <EQUITY/>) would otherwise blow up BigDecimal and Integer binding.
      .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
      // Which, left alone, would quietly make an empty tran_id or op_id a zero.
      .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
      // The feed pads values and writes an empty element where it means nothing, so absent and
      // blank are bound alike and nothing downstream trims or checks for "".
      .addModule(new SimpleModule().addDeserializer(String.class, new TrimToNullDeserializer()))
      .build();

  public static LicenceOperationHistory read(Reader in) throws IOException {
    var history = MAPPER.readValue(in, LicenceHistoryXml.History.class);
    return new LicenceOperationHistory(
        history.licenceType(),
        history.licenceNo(),
        PearsOperationMapper.toOperations(history)
    );
  }

  /**
   * Binds every string in the document trimmed, and a blank one as null.
   */
  private static final class TrimToNullDeserializer extends StdDeserializer<String> {

    private TrimToNullDeserializer() {
      super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
      var value = parser.getValueAsString();
      if (value == null) {
        return null;
      }
      var trimmed = value.trim();
      return trimmed.isEmpty() ? null : trimmed;
    }
  }
}

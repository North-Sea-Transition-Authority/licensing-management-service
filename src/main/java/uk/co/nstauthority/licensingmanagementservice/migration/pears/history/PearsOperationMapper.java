package uk.co.nstauthority.licensingmanagementservice.migration.pears.history;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the bound XML into the sealed {@link PearsOperation} hierarchy. Operation types vary
 * almost entirely by the names inside the generic ATTRIBUTE_LIST, so the discrimination happens
 * here, once, and everything downstream gets typed components.
 */
class PearsOperationMapper {

  static List<PearsOperation> toOperations(LicenceHistoryXml.History history) {
    if (history.entries() == null) {
      return List.of();
    }

    var operations = new ArrayList<PearsOperation>();
    for (var entry : history.entries()) {
      operations.add(toOperation(entry));
    }
    return operations;
  }

  static PearsOperation toOperation(LicenceHistoryXml.Entry entry) {
    var operation = entry.operation();

    // No payload means the query did not fetch this type's XML, so the entry's attributes are all
    // there is to go on -- including for the event date, which lives inside the payload.
    if (operation == null) {
      return new PearsOperation.Omitted(header(entry, Map.of()), entry.opType());
    }

    var attributes = flatten(operation);
    var header = header(entry, attributes);
    return switch (entry.opType()) {
      case PearsOperationType.CONSORTIUM_LIST_CREATE ->
          new PearsOperation.ConsortiumListCreate(
              header,
              operation.systemComment(),
              attributes.get("COMPANY_LIST_TYPE"),
              consortiumEntries(operation)
          );
      case PearsOperationType.CONSORTIUM_LIST_CHANGE ->
          new PearsOperation.ConsortiumListChange(
              header,
              operation.systemComment(),
              attributes.get("COMPANY_LIST_TYPE"),
              consortiumEntries(operation)
          );
      default -> new PearsOperation.Unrecognised(header, entry.opType(), attributes);
    };
  }

  /**
   * Flattens OPERATION/ATTRIBUTE_LIST into a map. An empty {@code <VALUE/>} binds to null, so it
   * becomes an absent key rather than an empty string and callers only null-check once.
   */
  private static Map<String, String> flatten(LicenceHistoryXml.Operation operation) {
    if (operation.attributeSets() == null || operation.attributeSets().isEmpty()) {
      return Map.of();
    }

    var attributes = new HashMap<String, String>();
    for (var attributeSet : operation.attributeSets()) {
      if (attributeSet.attributes() == null) {
        continue;
      }
      for (var attribute : attributeSet.attributes()) {
        if (attribute.name() != null && attribute.value() != null) {
          attributes.put(attribute.name(), attribute.value());
        }
      }
    }
    return attributes;
  }

  private static PearsOperation.Header header(LicenceHistoryXml.Entry entry, Map<String, String> attributes) {
    return new PearsOperation.Header(
        entry.tranId(),
        entry.regulatorReference(),
        date(entry.positionDate()),
        entry.positionSequence(),
        entry.opId(),
        entry.opSeq(),
        operationStatus(entry.opStatus()),
        date(attributes.get("EVENT_DATE")),
        attributes.get("LICENCE_TYPE")
    );
  }

  private static List<PearsOperation.ConsortiumEntry> consortiumEntries(LicenceHistoryXml.Operation operation) {
    if (operation.consortiumEntries() == null || operation.consortiumEntries().isEmpty()) {
      return List.of();
    }

    var entries = new ArrayList<PearsOperation.ConsortiumEntry>();
    for (var entry : operation.consortiumEntries()) {
      entries.add(new PearsOperation.ConsortiumEntry(
          entryType(entry.entryType()),
          organisation(entry.primaryOrganisation()),
          entry.primaryStatus(),
          organisation(entry.secondaryOrganisation())
      ));
    }
    return entries;
  }

  /**
   * The feed emits {@code <SECONDARY_ORGANISATION />} rather than omitting the element, so Jackson
   * hands back a non-null record with all-null components. That collapses back to absent.
   */
  private static PearsOperation.Organisation organisation(LicenceHistoryXml.Organisation organisation) {
    if (organisation == null || organisation.id() == null) {
      return null;
    }
    return new PearsOperation.Organisation(organisation.id(), organisation.name());
  }

  /**
   * {@code licence-history.sql} filters on exactly the statuses
   * {@link PearsOperation.OperationStatus} holds, so anything else means the query changed.
   */
  private static PearsOperation.OperationStatus operationStatus(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Missing required attribute op_status");
    }
    try {
      return PearsOperation.OperationStatus.valueOf(value.replace('-', '_').toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("PEARS operation status %s is not one licence-history.sql returns".formatted(value), e);
    }
  }

  /**
   * Null for an entry type PEARS has added since, which each migrator then reports in its own
   * terms rather than having it dropped here.
   */
  private static PearsOperation.EntryType entryType(String value) {
    if (value == null) {
      return null;
    }
    try {
      return PearsOperation.EntryType.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * A payload date carries a time part where the attribute it came from was a timestamp.
   */
  private static LocalDate date(String in) {
    return in == null ? null : LocalDate.parse(in.length() > 10 ? in.substring(0, 10) : in);
  }
}

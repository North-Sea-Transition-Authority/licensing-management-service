package uk.co.nstauthority.licensingmanagementservice.hibernate;

import java.io.Serial;
import java.lang.reflect.Member;
import java.util.EnumSet;
import java.util.UUID;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;

public class AssignedOrGeneratedUuidGenerator implements BeforeExecutionGenerator {

  @Serial
  private static final long serialVersionUID = 1L;

  public AssignedOrGeneratedUuidGenerator(
      AssignedOrGeneratedUuid config,
      Member idMember,
      CustomIdGeneratorCreationContext creationContext
  ) {
    // the annotation carries no configuration, so nothing to read off it
  }

  @Override
  public EnumSet<EventType> getEventTypes() {
    return EventTypeSets.INSERT_ONLY;
  }

  @Override
  public boolean allowAssignedIdentifiers() {
    return true;
  }

  @Override
  public Object generate(
      SharedSessionContractImplementor session,
      Object owner,
      Object currentValue,
      EventType eventType
  ) {
    return currentValue != null ? currentValue : UUID.randomUUID();
  }
}
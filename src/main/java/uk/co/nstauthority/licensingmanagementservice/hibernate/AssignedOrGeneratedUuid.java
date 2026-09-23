package uk.co.nstauthority.licensingmanagementservice.hibernate;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.hibernate.annotations.IdGeneratorType;

/**
 * Generates a random UUID on insert only when the entity has no id set, and keeps an id that was assigned before
 * persisting. Hibernate's {@code @UuidGenerator} always overwrites the id, which cannot be used when a correction is
 * applied: the positions, changes and transactions it adds are given their ids while the correction is staged, those
 * ids are held in the correction payload, and the live rows must be created with the same ids so the correction keeps
 * pointing at them. Entities created anywhere else leave the id null and get a generated one as before.
 */
@IdGeneratorType(AssignedOrGeneratedUuidGenerator.class)
@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface AssignedOrGeneratedUuid {
}
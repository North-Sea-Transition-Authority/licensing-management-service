package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationData;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@IntegrationTest
class ApplicationAuditIntegrationTest {

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @ParameterizedTest
  @ValueSource(classes = {
      ScheduleWorkProgrammeApplication.class,
      ScheduleWorkProgrammeApplicationData.class,
      ScheduleWorkProgrammeApplicationDetail.class,
      LicenceContinuationApplication.class,
      LicenceContinuationApplicationDetail.class,
      ApplicationSchedule.class
  })
  void auditEntity_whenEntityIsAudited_thenEveryPersistentPropertyIsAudited(Class<?> entityClass) {
    var mappingMetamodel = entityManagerFactory.unwrap(SessionFactoryImplementor.class).getMappingMetamodel();
    var entityPersister = (AbstractEntityPersister) mappingMetamodel.getEntityDescriptor(entityClass);

    var propertyNames = entityPersister.getPropertyNames();
    var entityProperties = IntStream.range(0, propertyNames.length)
        .filter(propertyIndex -> entityPersister.getPropertyColumnNames(propertyIndex).length > 0)
        .mapToObj(propertyIndex -> propertyNames[propertyIndex])
        .toList();
    var auditProperties = Arrays.stream(mappingMetamodel.getEntityDescriptor(entityClass.getName() + "_AUD").getPropertyNames())
        .map(propertyName -> StringUtils.removeEnd(propertyName, "_id"))
        .toList();

    assertThat(auditProperties).containsAll(entityProperties);
  }
}

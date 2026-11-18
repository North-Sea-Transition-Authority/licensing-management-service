package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@IntegrationTest
class ApplicationAuditIntegrationTest {

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @ParameterizedTest
  @ValueSource(classes = {
      ScheduleWorkProgrammeApplication.class,
      ScheduleWorkProgrammeApplicationDetail.class,
      LicenceContinuationApplication.class,
      LicenceContinuationApplicationDetail.class
  })
  void auditEntity_whenEntityIsAudited_thenEveryPersistentPropertyIsAudited(Class<?> entityClass) {
    var mappingMetamodel = entityManagerFactory.unwrap(SessionFactoryImplementor.class).getMappingMetamodel();

    var entityProperties = List.of(mappingMetamodel.getEntityDescriptor(entityClass).getPropertyNames());
    var auditProperties = Arrays.stream(mappingMetamodel.getEntityDescriptor(entityClass.getName() + "_AUD").getPropertyNames())
        .map(propertyName -> StringUtils.removeEnd(propertyName, "_id"))
        .toList();

    assertThat(auditProperties).containsAll(entityProperties);
  }
}

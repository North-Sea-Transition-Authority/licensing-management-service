package uk.co.nstauthority.licensingmanagementservice.migration.industryteam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Sql(
    scripts = "classpath:migration/create-pears-migration-tables.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@Transactional
@IntegrationTest
class PearsContactsMigrationExtractRepositoryIntegrationTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private PearsContactsMigrationExtractRepository pearsContactsMigrationExtractRepository;

  @Test
  void findAll_returnsExtractRowsKeyedByOrganisationGroupAndWuaId() {
    em.persist(new PearsContactsMigrationExtract(700, 7001));
    em.persist(new PearsContactsMigrationExtract(700, 7002));
    em.persist(new PearsContactsMigrationExtract(800, 7001));
    em.flush();

    assertThat(pearsContactsMigrationExtractRepository.findAll())
        .extracting(
            PearsContactsMigrationExtract::getOrganisationGroupId,
            PearsContactsMigrationExtract::getWuaId
        )
        .containsExactlyInAnyOrder(
            tuple(700, 7001),
            tuple(700, 7002),
            tuple(800, 7001)
        );
  }

  @Test
  void findById_resolvesARowByItsCompositeKey() {
    em.persist(new PearsContactsMigrationExtract(700, 7001));
    em.flush();

    assertThat(pearsContactsMigrationExtractRepository
        .findById(new PearsContactsMigrationExtractCompositeKey(700, 7001)))
        .isPresent();
    assertThat(pearsContactsMigrationExtractRepository
        .findById(new PearsContactsMigrationExtractCompositeKey(700, 9999)))
        .isEmpty();
  }
}

package uk.co.fivium.gisframework.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FeatureRepositoryIntegrationTest {

  @Autowired
  private FeatureRepository featureRepository;

  private Feature feature1;
  private Feature feature2;

  @BeforeEach
  void setup() {
    feature1 = FeatureTestUtil.newBuilder()
        .withId(null)
        .withFeatureName("feature1")
        .withLegacyId(1)
        .withAttributes(Map.of(
            "some_attribute_1", "some_value_1"
        ))
        .build();
    feature2 = FeatureTestUtil.newBuilder()
        .withId(null)
        .withFeatureName("feature2")
        .withLegacyId(2)
        .withAttributes(Map.of(
            "some_attribute_1", "some_value_2"
        ))
        .build();
    var feature3 = FeatureTestUtil.newBuilder()
        .withId(null)
        .withFeatureName("feature3")
        .withLegacyId(3)
        .withAttributes(Map.of(
            "some_attribute_2", "some_value_1"
        ))
        .build();

    featureRepository.saveAll(
        List.of(feature1, feature2, feature3)
    );
  }

  @Test
  void findAllByAttribute() {
    var result = featureRepository.findAllByAttribute("some_attribute_1", "some_value_1");
    var expectedResult = featureRepository.findById(feature1.getId()).orElseThrow();

    assertThat(result)
        .usingRecursiveComparison()
        .isEqualTo(List.of(expectedResult));
  }

  @Test
  void findAllByAttributeValueIn() {
    var result = featureRepository.findAllByAttributeValueIn("some_attribute_1", List.of("some_value_1", "some_value_2"));
    var expectedResult = List.of(
        featureRepository.findById(feature1.getId()).orElseThrow(),
        featureRepository.findById(feature2.getId()).orElseThrow()
    );

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(expectedResult);
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = Feature.class)
  @EnableJpaRepositories(basePackageClasses = FeatureRepository.class)
  static class TestApplication {
  }
}

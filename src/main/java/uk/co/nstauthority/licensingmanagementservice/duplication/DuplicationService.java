package uk.co.nstauthority.licensingmanagementservice.duplication;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.util.ReflectionUtil;

@Service
public class DuplicationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DuplicationService.class);

  static final String ID_FIELD_NAME = "id";

  static final String FIND_REPOSITORY_METHOD_ERROR_MESSAGE =
      "Cannot find repository method with annotation %s in %s for duplication processing";

  static final String UNEXPECTED_REPOSITORY_METHOD_RETURN_TYPE_ERROR_MESSAGE =
      "Cannot invoke repository method %s as the return type %s is unexpected";

  private final EntityManager entityManager;

  public DuplicationService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Transactional
  public <T> void duplicateChildEntities(
      T oldParentEntity,
      T newParentEntity,
      List<DuplicationSource<T>> duplicationSources
  ) {
    LOGGER.info("Duplicating child entities for {} into {}", oldParentEntity, newParentEntity);

    // duplicate all data where the repository extends DuplicationSource<T>
    for (var duplicationSource : duplicationSources) {
      var repoMethod = ReflectionUtil.getAllMethods(duplicationSource.getClass())
          .stream()
          .filter(method -> ((AnnotationUtils.findAnnotation(method, DuplicateThisOnUpdate.class)) != null))
          .findFirst()
          .orElseThrow(() -> new RuntimeException(FIND_REPOSITORY_METHOD_ERROR_MESSAGE
              .formatted(DuplicateThisOnUpdate.class.getName(), duplicationSource.getClass().getName())));

      try {
        if (repoMethod.getReturnType().equals(Optional.class)) {
          var entityToDuplicateOptional = (Optional<?>) repoMethod.invoke(duplicationSource, oldParentEntity);
          entityToDuplicateOptional.ifPresent(entityToDuplicate ->
              duplicateEntityAndSetParentEntity(newParentEntity, entityToDuplicate));

        } else if (repoMethod.getReturnType().equals(List.class)) {
          var entitiesToDuplicate = (List<?>) repoMethod.invoke(duplicationSource, oldParentEntity);
          entitiesToDuplicate.forEach(entityToDuplicate ->
              duplicateEntityAndSetParentEntity(newParentEntity, entityToDuplicate));
        } else {
          throw new RuntimeException(UNEXPECTED_REPOSITORY_METHOD_RETURN_TYPE_ERROR_MESSAGE
              .formatted(repoMethod.getName(), repoMethod.getReturnType().getName()));
        }
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T, D> void duplicateEntityAndSetParentEntity(T parentEntity, D duplicationSource) {
    D target = DuplicationUtil.instantiateBlankInstance((Class<D>) duplicationSource.getClass());
    DuplicationUtil.copyProperties(duplicationSource, target, ID_FIELD_NAME);
    ((LinkedToDuplicationParent<T>) target).setDuplicationParent(parentEntity);
    entityManager.persist(target);
  }
}

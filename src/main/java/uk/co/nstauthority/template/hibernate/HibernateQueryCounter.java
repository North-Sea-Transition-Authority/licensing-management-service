package uk.co.nstauthority.template.hibernate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

@Component
public class HibernateQueryCounter implements StatementInspector {

  /* replace xyz with service schema name*/
  static final Pattern TABLE_PATTERN = Pattern.compile("from xyz.([^?| ]+)");
  static final Pattern CONDITION_PATTERN = Pattern.compile("\\b(where|and|or)\\b \\w+\\.(\\w+)");
  private final transient ThreadLocal<Map<String, Long>> sqlToCount = ThreadLocal.withInitial(HashMap::new);


  public Map<String, Long> getSqlToCount() {
    return sqlToCount.get();
  }

  public Long getOverallQueryCount() {
    return sqlToCount.get()
        .values()
        .stream()
        .mapToLong(value -> value)
        .sum();
  }

  public void clearQueryCount() {
    sqlToCount.remove();
  }

  @Override
  public String inspect(String sql) {
    var map = sqlToCount.get();
    var countForPurpose = map.get(formatQuery(sql));
    if (!map.isEmpty() && countForPurpose != null) {
      map.put(formatQuery(sql), countForPurpose + 1);
    } else {
      map.put(formatQuery(sql), 1L);
    }
    return sql;
  }

  private static String formatQuery(String query) {
    var matchedTableNames = TABLE_PATTERN.matcher(query).results().findFirst().orElse(null);
    var tableName = Objects.isNull(matchedTableNames) ? "" : matchedTableNames.group(1);
    var conditions = CONDITION_PATTERN.matcher(query).results().map(group -> group.group(2)).toList();
    StringBuilder conditionString = new StringBuilder();

    if (!conditions.isEmpty()) {
      conditionString = new StringBuilder("by %s".formatted(conditions.getFirst()));

      if (conditions.size() > 1) {
        for (int i = 1; i < conditions.size(); i++) {
          conditionString.append(", %s".formatted(conditions.get(i)));
        }
      }
    }
    return "find %s %s".formatted(tableName, conditionString.toString());

  }

}

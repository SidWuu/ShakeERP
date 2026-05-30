package com.factory.erp.common;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Service 基类，提供各模块共用的工具方法。
 * 所有业务 Service 继承此类以复用编码生成、日期过滤等逻辑。
 */
public abstract class BaseService {

    protected final JdbcTemplate jdbcTemplate;

    protected BaseService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 生成下一个自增编码。
     * 格式为 prefix-XXXX，例如 P-1001、C-1002。
     * 同时从内存记录和数据库中取最大值，避免主键冲突。
     *
     * @param prefix  编码前缀
     * @param records 当前已有记录
     * @param table   数据库表名
     * @return 新编码
     */
    protected String nextCode(String prefix, Map<String, ?> records, String table) {
        // 从内存取最大值
        int maxMemory = records.keySet().stream()
                .filter(code -> code.startsWith(prefix + "-"))
                .map(code -> code.substring((prefix + "-").length()))
                .filter(value -> value.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(1000);

        // 从数据库取最大值
        int maxDb = 1000;
        try {
            String sql = "select max(cast(substr(code, %d) as integer)) from %s where code like '%s-%%'"
                    .formatted(prefix.length() + 2, table, prefix);
            Integer dbVal = jdbcTemplate.queryForObject(sql, Integer.class);
            if (dbVal != null) {
                maxDb = dbVal;
            }
        } catch (Exception ignored) {
        }

        int max = Math.max(maxMemory, maxDb);
        return "%s-%04d".formatted(prefix, max + 1);
    }

    /** 兼容旧调用（不查数据库） */
    protected String nextCode(String prefix, Map<String, ?> records) {
        return nextCode(prefix, records, "");
    }

    /**
     * 模糊匹配：判断 value 是否包含 query（忽略大小写）。
     * query 为空时视为匹配所有。
     */
    protected boolean contains(String value, String query) {
        return query == null || query.isBlank() || value.toLowerCase().contains(query.toLowerCase());
    }

    /**
     * 日期范围过滤：判断 dateTime 是否在 [startDate, endDate] 范围内。
     * startDate 或 endDate 为空时不限制对应边界。
     */
    protected boolean matchesDate(String dateTime, String startDate, String endDate) {
        if (dateTime == null) {
            return false;
        }
        LocalDate date = OffsetDateTime.parse(dateTime).toLocalDate();
        if (startDate != null && !startDate.isBlank() && date.isBefore(LocalDate.parse(startDate))) {
            return false;
        }
        return endDate == null || endDate.isBlank() || !date.isAfter(LocalDate.parse(endDate));
    }

    /** 获取当前时间的 ISO 8601 字符串。 */
    protected String now() {
        return OffsetDateTime.now().toString();
    }
}

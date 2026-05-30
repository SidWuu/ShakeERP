package com.factory.erp.admin;

import com.factory.erp.common.ApiResponse;
import com.factory.erp.common.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统管理 API。
 * 提供数据库备份、恢复、数据字典、用户列表功能。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    private final JdbcTemplate jdbcTemplate;

    public AdminController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initDictSchema();
    }

    private void initDictSchema() {
        try {
            jdbcTemplate.execute("""
                create table if not exists dict_categories (
                    id integer primary key autoincrement,
                    name text not null unique
                )
            """);
            jdbcTemplate.execute("""
                create table if not exists dict_items (
                    id integer primary key autoincrement,
                    category text not null,
                    label text not null,
                    value text not null
                )
            """);
        } catch (Exception ignored) {
        }
    }

    /** 创建数据库备份 */
    @PostMapping("/backup")
    public ApiResponse<Map<String, String>> backup() {
        String backupName = doBackup();
        return ApiResponse.ok(Map.of("filename", backupName));
    }

    /** 获取备份列表 */
    @GetMapping("/backups")
    public ApiResponse<List<Map<String, Object>>> listBackups() {
        Path backupDir = getBackupDir();
        if (!Files.exists(backupDir)) {
            return ApiResponse.ok(List.of());
        }

        try (Stream<Path> files = Files.list(backupDir)) {
            List<Map<String, Object>> backups = files
                    .filter(f -> f.toString().endsWith(".db"))
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .map(f -> {
                        try {
                            return Map.<String, Object>of(
                                    "filename", f.getFileName().toString(),
                                    "size", Files.size(f),
                                    "createdAt", Files.getLastModifiedTime(f).toString()
                            );
                        } catch (IOException e) {
                            return Map.<String, Object>of("filename", f.getFileName().toString());
                        }
                    })
                    .toList();
            return ApiResponse.ok(backups);
        } catch (IOException e) {
            throw new BusinessException("读取备份列表失败：" + e.getMessage());
        }
    }

    /** 从备份恢复数据库（先备份当前数据库，再用指定备份覆盖） */
    @PostMapping("/restore/{filename}")
    public ApiResponse<Map<String, String>> restore(@PathVariable String filename) {
        Path backupDir = getBackupDir();
        Path backupFile = backupDir.resolve(filename);

        if (!Files.exists(backupFile)) {
            throw new BusinessException("备份文件不存在：" + filename);
        }

        // 安全检查：防止路径穿越
        if (!backupFile.normalize().startsWith(backupDir.normalize())) {
            throw new BusinessException("非法文件路径");
        }

        // 先备份当前数据库
        String safeBackup = doBackup("pre_restore_");

        // 用备份文件覆盖当前数据库
        Path dbPath = getDbPath();
        try {
            Files.copy(backupFile, dbPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("恢复失败：" + e.getMessage());
        }

        return ApiResponse.ok(Map.of(
                "restored", filename,
                "previousBackup", safeBackup,
                "message", "恢复成功，请重启后端服务使数据生效"
        ));
    }

    /** 删除指定备份文件 */
    @DeleteMapping("/backups/{filename}")
    public ApiResponse<Map<String, String>> deleteBackup(@PathVariable String filename) {
        Path backupDir = getBackupDir();
        Path backupFile = backupDir.resolve(filename);

        if (!backupFile.normalize().startsWith(backupDir.normalize())) {
            throw new BusinessException("非法文件路径");
        }
        if (!Files.exists(backupFile)) {
            throw new BusinessException("备份文件不存在：" + filename);
        }

        try {
            Files.delete(backupFile);
        } catch (IOException e) {
            throw new BusinessException("删除失败：" + e.getMessage());
        }
        return ApiResponse.ok(Map.of("deleted", filename));
    }

    /** 下载当前数据库文件 */
    @GetMapping("/export")
    public ResponseEntity<Resource> exportDb() {
        Path dbPath = getDbPath();
        if (!Files.exists(dbPath)) {
            throw new BusinessException("数据库文件不存在");
        }
        Resource resource = new FileSystemResource(dbPath.toFile());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"factory-erp.db\"")
                .body(resource);
    }

    /** 获取所有字典分类 */
    @GetMapping("/dict/categories")
    public ApiResponse<List<Map<String, Object>>> listDictCategories() {
        List<Map<String, Object>> categories = jdbcTemplate.queryForList("select id, name from dict_categories order by id");
        return ApiResponse.ok(categories);
    }

    /** 新增字典分类 */
    @PostMapping("/dict/categories")
    public ApiResponse<Map<String, String>> createDictCategory(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new BusinessException("分类名称不能为空");
        }
        try {
            jdbcTemplate.update("insert into dict_categories (name) values (?)", name.trim());
        } catch (Exception e) {
            throw new BusinessException("分类已存在");
        }
        return ApiResponse.ok(Map.of("name", name.trim()));
    }

    /** 删除字典分类（如果该分类下有字典项则拒绝删除） */
    @DeleteMapping("/dict/categories/{id}")
    public ApiResponse<Map<String, String>> deleteDictCategory(@PathVariable int id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select name from dict_categories where id = ?", id);
        if (rows.isEmpty()) {
            throw new BusinessException("分类不存在");
        }
        String name = (String) rows.get(0).get("name");
        Integer count = jdbcTemplate.queryForObject("select count(*) from dict_items where category = ?", Integer.class, name);
        if (count != null && count > 0) {
            throw new BusinessException("分类「" + name + "」下还有 " + count + " 条字典数据，请先手动删除后再删除分类");
        }
        jdbcTemplate.update("delete from dict_categories where id = ?", id);
        return ApiResponse.ok(Map.of("deleted", name));
    }

    /** 获取字典项列表（支持按分类筛选） */
    @GetMapping("/dict")
    public ApiResponse<List<Map<String, Object>>> listDictItems(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String category
    ) {
        if (category != null && !category.isBlank()) {
            return ApiResponse.ok(jdbcTemplate.queryForList(
                    "select id, category, label, value from dict_items where category = ? order by id", category));
        }
        return ApiResponse.ok(jdbcTemplate.queryForList("select id, category, label, value from dict_items order by category, id"));
    }

    /** 新增字典项 */
    @PostMapping("/dict")
    public ApiResponse<Map<String, Object>> createDictItem(@RequestBody Map<String, String> body) {
        String category = body.get("category");
        String label = body.get("label");
        String value = body.get("value");
        if (category == null || category.isBlank() || label == null || label.isBlank()) {
            throw new BusinessException("分类和名称不能为空");
        }
        if (value == null || value.isBlank()) {
            value = label;
        }
        jdbcTemplate.update("insert into dict_items (category, label, value) values (?, ?, ?)", category, label, value);
        return ApiResponse.ok(Map.of("category", category, "label", label, "value", value));
    }

    /** 删除字典项 */
    @DeleteMapping("/dict/{id}")
    public ApiResponse<Map<String, String>> deleteDictItem(@PathVariable int id) {
        int rows = jdbcTemplate.update("delete from dict_items where id = ?", id);
        if (rows == 0) {
            throw new BusinessException("字典项不存在");
        }
        return ApiResponse.ok(Map.of("deleted", String.valueOf(id)));
    }

    /** 获取用户列表（不含密码） */
    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> listUsers() {
        List<Map<String, Object>> users = jdbcTemplate.queryForList("select username, display_name as displayName, role from users order by username");
        return ApiResponse.ok(users);
    }

    /** 执行备份，返回备份文件名 */
    public String doBackup() {
        return doBackup("");
    }

    /** 执行备份（带前缀），返回备份文件名 */
    public String doBackup(String prefix) {
        Path dbPath = getDbPath();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupName = prefix + "factory-erp_" + timestamp + ".db";
        Path backupDir = getBackupDir();

        try {
            Files.createDirectories(backupDir);
            Path backupPath = backupDir.resolve(backupName);
            Files.copy(dbPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            return backupName;
        } catch (IOException e) {
            throw new BusinessException("备份失败：" + e.getMessage());
        }
    }

    private Path getBackupDir() {
        return getDbPath().getParent().resolve("backups");
    }

    /** 供 BackupScheduler 调用 */
    public Path doGetBackupDir() {
        return getBackupDir();
    }

    private Path getDbPath() {
        String path = datasourceUrl.replace("jdbc:sqlite:", "");
        return Paths.get(path).toAbsolutePath().normalize();
    }
}

package com.factory.erp.admin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据库自动备份定时任务。
 * 每天凌晨 2:00 自动备份数据库，保留最近 30 个备份文件。
 */
@Component
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);
    private static final int MAX_BACKUPS = 30;

    private final AdminController adminController;

    public BackupScheduler(AdminController adminController) {
        this.adminController = adminController;
    }

    /** 每天凌晨 2:00 执行自动备份 */
    @Scheduled(cron = "0 0 2 * * *")
    public void dailyBackup() {
        try {
            String filename = adminController.doBackup("auto_");
            log.info("每日自动备份完成：{}", filename);
            cleanOldBackups();
        } catch (Exception e) {
            log.error("每日自动备份失败：{}", e.getMessage(), e);
        }
    }

    /** 清理超过 MAX_BACKUPS 个的旧自动备份 */
    private void cleanOldBackups() {
        try {
            Path backupDir = adminController.doGetBackupDir();
            if (!Files.exists(backupDir)) return;

            try (Stream<Path> files = Files.list(backupDir)) {
                var autoBackups = files
                        .filter(f -> f.getFileName().toString().startsWith("auto_"))
                        .sorted(Comparator.comparing(f -> f.getFileName().toString(), Comparator.reverseOrder()))
                        .toList();

                if (autoBackups.size() > MAX_BACKUPS) {
                    for (int i = MAX_BACKUPS; i < autoBackups.size(); i++) {
                        Files.deleteIfExists(autoBackups.get(i));
                        log.info("清理旧备份：{}", autoBackups.get(i).getFileName());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("清理旧备份失败：{}", e.getMessage());
        }
    }
}

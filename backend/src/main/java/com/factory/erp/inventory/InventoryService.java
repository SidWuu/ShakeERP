package com.factory.erp.inventory;

import com.factory.erp.common.BaseService;
import com.factory.erp.common.exception.BusinessException;
import com.factory.erp.common.exception.NotFoundException;
import com.factory.erp.inventory.InventoryController.InventoryRequest;
import com.factory.erp.inventory.InventoryController.InventorySummary;
import com.factory.erp.product.ProductService;
import com.factory.erp.product.ProductService.ProductRecord;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 库存管理 Service。
 * 负责库存汇总、入库、出库、库存台账等操作。
 * 依赖 ProductService 获取商品信息，数据持久化到 SQLite inventory 和 stock_movements 表。
 */
@Service
public class InventoryService extends BaseService {

    private final ProductService productService;
    private final Map<String, Integer> stockByProductCode = new LinkedHashMap<>();
    private final Map<String, String> inventoryCodeByProductCode = new LinkedHashMap<>();
    private final Map<String, String> inventoryChangedAtByProductCode = new LinkedHashMap<>();
    private final List<StockMovement> movements = new ArrayList<>();

    public InventoryService(JdbcTemplate jdbcTemplate, ProductService productService) {
        super(jdbcTemplate);
        this.productService = productService;
    }

    public synchronized void reloadFromDatabase() {
        loadFromDatabase();
    }

    public synchronized List<InventorySummary> summary(String startDate, String endDate, String code, String name) {
        return productService.list(null, null, null, null).stream()
                .filter(p -> matchesDate(inventoryChangedAtByProductCode.get(p.code()), startDate, endDate))
                .filter(p -> contains(p.code(), code))
                .filter(p -> contains(p.name(), name))
                .map(p -> toSummary(p.code(), p.name(), p.unit(), p.safetyStock()))
                .toList();
    }

    public synchronized InventorySummary saveInventory(String inventoryCode, InventoryRequest request) {
        ProductRecord product = productService.requireProduct(request.productCode());
        String currentInventoryCode = inventoryCodeByProductCode.get(product.code());
        if (inventoryCode != null && !inventoryCode.isBlank() && !inventoryCode.equals(currentInventoryCode)) {
            throw new NotFoundException("库存不存在");
        }

        int currentQuantity = stockByProductCode.getOrDefault(product.code(), 0);
        int nextQuantity = request.quantity();
        stockByProductCode.put(product.code(), nextQuantity);
        String changedAt = now();
        inventoryChangedAtByProductCode.put(product.code(), changedAt);
        if (currentInventoryCode == null) {
            currentInventoryCode = nextInventoryCode();
            inventoryCodeByProductCode.put(product.code(), currentInventoryCode);
        }
        upsertInventory(product.code(), currentInventoryCode, nextQuantity, changedAt);
        String operator = request.operator() == null || request.operator().isBlank() ? "系统管理员" : request.operator();
        StockMovement movement = StockMovement.inventorySave(
                product.code(), product.name(), currentQuantity, nextQuantity, operator, request.remark(), changedAt
        );
        movements.add(movement);
        insertMovement(movement);
        return toSummary(product.code(), product.name(), product.unit(), product.safetyStock());
    }

    public synchronized InventorySummary stockIn(StockActionRequest request) {
        ProductRecord product = productService.requireProduct(request.productCode());
        int currentQuantity = stockByProductCode.getOrDefault(product.code(), 0);
        int nextQuantity = currentQuantity + request.quantity();
        stockByProductCode.put(product.code(), nextQuantity);
        String changedAt = now();
        inventoryChangedAtByProductCode.put(product.code(), changedAt);
        String invCode = inventoryCodeByProductCode.get(product.code());
        if (invCode == null) {
            invCode = nextInventoryCode();
            inventoryCodeByProductCode.put(product.code(), invCode);
        }
        upsertInventory(product.code(), invCode, nextQuantity, changedAt);
        StockMovement movement = StockMovement.in(
                product.code(), product.name(), request.quantity(), currentQuantity, nextQuantity, request.operator(), request.remark(), request.customerCode(), changedAt
        );
        movements.add(movement);
        insertMovement(movement);
        return toSummary(product.code(), product.name(), product.unit(), product.safetyStock());
    }

    public synchronized InventorySummary stockOut(StockActionRequest request) {
        ProductRecord product = productService.requireProduct(request.productCode());
        int currentQuantity = stockByProductCode.getOrDefault(product.code(), 0);
        if (currentQuantity < request.quantity()) {
            throw new BusinessException("库存不足");
        }
        int nextQuantity = currentQuantity - request.quantity();
        stockByProductCode.put(product.code(), nextQuantity);
        String changedAt = now();
        inventoryChangedAtByProductCode.put(product.code(), changedAt);
        upsertInventory(product.code(), inventoryCodeByProductCode.get(product.code()), nextQuantity, changedAt);
        StockMovement movement = StockMovement.out(
                product.code(), product.name(), request.quantity(), currentQuantity, nextQuantity, request.operator(), request.remark(), request.customerCode(), changedAt
        );
        movements.add(movement);
        insertMovement(movement);
        return toSummary(product.code(), product.name(), product.unit(), product.safetyStock());
    }

    public synchronized InventorySummary deleteInventory(String productCode, String operator) {
        ProductRecord product = productService.requireProduct(productCode);
        int currentQuantity = stockByProductCode.getOrDefault(product.code(), 0);
        String changedAt = now();

        // 写入台账记录
        StockMovement movement = StockMovement.inventoryDelete(product.code(), product.name(), currentQuantity, operator, changedAt);
        movements.add(movement);
        insertMovement(movement);

        // 删除库存记录（而非置零）
        stockByProductCode.remove(product.code());
        inventoryCodeByProductCode.remove(product.code());
        inventoryChangedAtByProductCode.remove(product.code());
        jdbcTemplate.update("delete from inventory where product_code = ?", product.code());

        return new InventorySummary(null, product.code(), product.name(), 0, product.unit(), product.safetyStock(), changedAt);
    }

    public synchronized void removeProductInventory(String productCode) {
        stockByProductCode.remove(productCode);
        inventoryCodeByProductCode.remove(productCode);
        inventoryChangedAtByProductCode.remove(productCode);
        jdbcTemplate.update("delete from inventory where product_code = ?", productCode);
    }

    public synchronized List<StockMovement> movements(String startDate, String endDate, String code, String name) {
        return movements.stream()
                .filter(m -> matchesDate(m.createdAt(), startDate, endDate))
                .filter(m -> contains(m.productCode(), code))
                .filter(m -> contains(m.productName(), name))
                .toList();
    }

    public synchronized int getStock(String productCode) {
        return stockByProductCode.getOrDefault(productCode, 0);
    }

    // --- private helpers ---

    private InventorySummary toSummary(String productCode, String productName, String unit, int safetyStock) {
        return new InventorySummary(
                inventoryCodeByProductCode.get(productCode),
                productCode,
                productName,
                stockByProductCode.getOrDefault(productCode, 0),
                unit,
                safetyStock,
                inventoryChangedAtByProductCode.get(productCode)
        );
    }

    private String nextInventoryCode() {
        int max = inventoryCodeByProductCode.values().stream()
                .filter(code -> code.startsWith("I-"))
                .map(code -> code.substring(2))
                .filter(value -> value.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(1000);
        return "I-%04d".formatted(max + 1);
    }

    private void loadFromDatabase() {
        stockByProductCode.clear();
        inventoryCodeByProductCode.clear();
        inventoryChangedAtByProductCode.clear();
        movements.clear();

        jdbcTemplate.query("select product_code, code, quantity, last_changed_at from inventory order by code", (java.sql.ResultSet rs) -> {
            String productCode = rs.getString("product_code");
            inventoryCodeByProductCode.put(productCode, rs.getString("code"));
            stockByProductCode.put(productCode, rs.getInt("quantity"));
            inventoryChangedAtByProductCode.put(productCode, rs.getString("last_changed_at"));
        });
        jdbcTemplate.query("select type, product_code, product_name, quantity, before_quantity, after_quantity, operator, remark, customer_code, created_at from stock_movements order by id", (java.sql.ResultSet rs) -> {
            movements.add(new StockMovement(
                    rs.getString("type"),
                    rs.getString("product_code"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getInt("before_quantity"),
                    rs.getInt("after_quantity"),
                    rs.getString("operator"),
                    rs.getString("remark"),
                    rs.getString("customer_code"),
                    rs.getString("created_at")
            ));
        });
    }

    private void upsertInventory(String productCode, String inventoryCode, int quantity, String changedAt) {
        jdbcTemplate.update("""
                insert into inventory (product_code, code, quantity, last_changed_at)
                values (?, ?, ?, ?)
                on conflict(product_code) do update set
                    code = excluded.code, quantity = excluded.quantity, last_changed_at = excluded.last_changed_at
                """, productCode, inventoryCode, quantity, changedAt);
    }

    private void insertMovement(StockMovement movement) {
        jdbcTemplate.update("""
                insert into stock_movements (type, product_code, product_name, quantity, before_quantity, after_quantity, operator, remark, customer_code, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                movement.type(), movement.productCode(), movement.productName(), movement.quantity(),
                movement.beforeQuantity(), movement.afterQuantity(), movement.operator(), movement.remark(), movement.customerCode(), movement.createdAt());
    }

    // --- records ---

    public record StockActionRequest(String productCode, int quantity, String operator, String remark, String customerCode) {}

    public record ScannerProduct(String code, String barcode, String name, String category, String unit, int safetyStock, int quantity) {}

    public record StockMovement(
            String type, String productCode, String productName,
            int quantity, int beforeQuantity, int afterQuantity,
            String operator, String remark, String customerCode, String createdAt
    ) {
        static StockMovement in(String productCode, String productName, int quantity, int before, int after, String operator, String remark, String customerCode, String createdAt) {
            return new StockMovement("新增", productCode, productName, quantity, before, after, operator, remark, customerCode, createdAt);
        }

        static StockMovement out(String productCode, String productName, int quantity, int before, int after, String operator, String remark, String customerCode, String createdAt) {
            return new StockMovement("修改", productCode, productName, quantity, before, after, operator, remark, customerCode, createdAt);
        }

        static StockMovement inventoryDelete(String productCode, String productName, int before, String operator, String createdAt) {
            return new StockMovement("删除", productCode, productName, before, before, 0, operator, "删除库存", null, createdAt);
        }

        static StockMovement inventorySave(String productCode, String productName, int before, int after, String operator, String remark, String createdAt) {
            return new StockMovement("修改", productCode, productName, Math.abs(after - before), before, after, operator, remark, null, createdAt);
        }
    }
}

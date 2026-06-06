package com.factory.erp.product;

import com.factory.erp.common.BaseService;
import com.factory.erp.common.exception.BusinessException;
import com.factory.erp.common.exception.NotFoundException;
import com.factory.erp.product.ProductController.ProductRequest;
import com.factory.erp.product.ProductController.ProductSummary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 商品管理 Service。
 * 负责商品的 CRUD 操作和条码查询，数据持久化到 SQLite products 表。
 */
@Service
public class ProductService extends BaseService {

    private final Map<String, ProductRecord> products = new LinkedHashMap<>();

    public ProductService(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public synchronized void reloadFromDatabase() {
        loadFromDatabase();
    }

    public synchronized List<ProductSummary> list(String startDate, String endDate, String code, String name) {
        return products.values().stream()
                .filter(p -> matchesDate(p.createdAt(), startDate, endDate))
                .filter(p -> contains(p.code(), code))
                .filter(p -> contains(p.name(), name))
                .map(ProductRecord::toSummary)
                .toList();
    }

    public synchronized ProductSummary create(ProductRequest request) {
        String code = nextCode("P", products, "products");
        if (request.barcode() != null && !request.barcode().isBlank() && findByBarcodeInternal(request.barcode()) != null) {
            throw new BusinessException("条码已存在");
        }

        ProductRecord product = new ProductRecord(
                code,
                request.barcode() != null ? request.barcode() : "",
                request.name(),
                request.category() != null ? request.category() : "",
                request.unit() != null && !request.unit().isBlank() ? request.unit() : "",
                request.safetyStock(),
                now(),
                request.imageUrl()
        );
        products.put(product.code(), product);
        insertProduct(product);
        return product.toSummary();
    }

    public synchronized ProductSummary update(String productCode, ProductRequest request) {
        ProductRecord current = requireProduct(productCode);
        if (request.barcode() != null && !request.barcode().isBlank()) {
            ProductRecord byBarcode = findByBarcodeInternal(request.barcode());
            if (byBarcode != null && !byBarcode.code().equals(productCode)) {
                throw new BusinessException("条码已存在");
            }
        }

        ProductRecord updated = new ProductRecord(
                current.code(),
                request.barcode() != null ? request.barcode() : current.barcode(),
                request.name(),
                request.category() != null ? request.category() : current.category(),
                request.unit() != null && !request.unit().isBlank() ? request.unit() : current.unit(),
                request.safetyStock(),
                current.createdAt(),
                request.imageUrl() != null ? request.imageUrl() : current.imageUrl()
        );
        products.put(updated.code(), updated);
        updateProductRecord(updated);
        return updated.toSummary();
    }

    public synchronized ProductSummary delete(String productCode) {
        ProductRecord product = requireProduct(productCode);
        products.remove(productCode);
        deleteProductRecord(productCode);
        return product.toSummary();
    }

    public synchronized ProductRecord requireProduct(String productCode) {
        ProductRecord product = products.get(productCode);
        if (product == null) {
            throw new NotFoundException("商品不存在");
        }
        return product;
    }

    public synchronized ProductRecord findByBarcode(String barcode) {
        return findByBarcodeInternal(barcode);
    }

    public synchronized boolean isEmpty() {
        return products.isEmpty();
    }

    private ProductRecord findByBarcodeInternal(String barcode) {
        return products.values().stream()
                .filter(p -> p.barcode().equals(barcode))
                .findFirst()
                .orElse(null);
    }

    private void loadFromDatabase() {
        products.clear();
        jdbcTemplate.query("select code, barcode, name, category, unit, safety_stock, created_at, image_url from products order by code", (java.sql.ResultSet rs) -> {
            ProductRecord product = new ProductRecord(
                    rs.getString("code"),
                    rs.getString("barcode"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getString("unit"),
                    rs.getInt("safety_stock"),
                    rs.getString("created_at"),
                    rs.getString("image_url")
            );
            products.put(product.code(), product);
        });
    }

    private void insertProduct(ProductRecord product) {
        jdbcTemplate.update(
                "insert into products (code, barcode, name, category, unit, safety_stock, created_at, image_url) values (?, ?, ?, ?, ?, ?, ?, ?)",
                product.code(), product.barcode(), product.name(), product.category(), product.unit(), product.safetyStock(), product.createdAt(), product.imageUrl()
        );
    }

    private void updateProductRecord(ProductRecord product) {
        jdbcTemplate.update(
                "update products set barcode = ?, name = ?, category = ?, unit = ?, safety_stock = ?, image_url = ? where code = ?",
                product.barcode(), product.name(), product.category(), product.unit(), product.safetyStock(), product.imageUrl(), product.code()
        );
    }

    private void deleteProductRecord(String productCode) {
        jdbcTemplate.update("delete from products where code = ?", productCode);
    }

    public record ProductRecord(
            String code,
            String barcode,
            String name,
            String category,
            String unit,
            int safetyStock,
            String createdAt,
            String imageUrl
    ) {
        public ProductSummary toSummary() {
            return new ProductSummary(code, barcode, name, category, unit, safetyStock, createdAt, imageUrl);
        }
    }
}

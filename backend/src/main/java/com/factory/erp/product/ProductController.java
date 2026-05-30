package com.factory.erp.product;

import com.factory.erp.common.ApiResponse;
import com.factory.erp.inventory.InventoryService;
import com.factory.erp.inventory.InventoryService.StockActionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品管理 API。
 * 提供商品的新增、查询、更新、删除接口。
 * 新增/更新商品时如果填写了增加库存数量，会同步写入库存。
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final InventoryService inventoryService;

    public ProductController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ApiResponse<List<ProductSummary>> listProducts(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name
    ) {
        return ApiResponse.ok(productService.list(startDate, endDate, code, name));
    }

    @PostMapping
    public ApiResponse<ProductSummary> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductSummary saved = productService.create(request);
        if (request.initialQuantity() > 0) {
            inventoryService.stockIn(new StockActionRequest(saved.code(), request.initialQuantity(), "系统管理员", "商品新增库存", null));
        }
        return ApiResponse.ok(saved);
    }

    @PutMapping("/{code}")
    public ApiResponse<ProductSummary> updateProduct(
            @PathVariable String code,
            @Valid @RequestBody ProductRequest request
    ) {
        ProductSummary saved = productService.update(code, request);
        if (request.initialQuantity() > 0) {
            inventoryService.stockIn(new StockActionRequest(saved.code(), request.initialQuantity(), "系统管理员", "商品页增加库存", null));
        }
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/{code}")
    public ApiResponse<ProductSummary> deleteProduct(@PathVariable String code) {
        ProductSummary deleted = productService.delete(code);
        inventoryService.removeProductInventory(code);
        return ApiResponse.ok(deleted);
    }

    public record ProductSummary(
            String code,
            String barcode,
            String name,
            String category,
            String unit,
            int safetyStock,
            String createdAt,
            String imageUrl
    ) {
    }

    public record ProductRequest(
            String code,
            String barcode,
            @NotBlank(message = "商品名称不能为空") String name,
            String category,
            String unit,
            @Min(value = 0, message = "安全库存不能为负数") int safetyStock,
            @Min(value = 0, message = "增加库存不能为负数") int initialQuantity,
            String imageUrl
    ) {
    }
}

package com.factory.erp.inventory;

import com.factory.erp.common.ApiResponse;
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
 * 库存管理 API。
 * 提供库存汇总查询、库存保存/更新、库存删除（清零）接口。
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/summary")
    public ApiResponse<List<InventorySummary>> summary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name
    ) {
        return ApiResponse.ok(inventoryService.summary(startDate, endDate, code, name));
    }

    @PostMapping
    public ApiResponse<InventorySummary> saveInventory(@Valid @RequestBody InventoryRequest request) {
        return ApiResponse.ok(inventoryService.saveInventory(null, request));
    }

    @PutMapping("/{code}")
    public ApiResponse<InventorySummary> updateInventory(
            @PathVariable String code,
            @Valid @RequestBody InventoryRequest request
    ) {
        return ApiResponse.ok(inventoryService.saveInventory(code, request));
    }

    @DeleteMapping("/{productCode}")
    public ApiResponse<InventorySummary> deleteInventory(
            @PathVariable String productCode,
            @RequestParam(defaultValue = "系统管理员") String operator
    ) {
        return ApiResponse.ok(inventoryService.deleteInventory(productCode, operator));
    }

    public record InventorySummary(
            String code,
            String productCode,
            String productName,
            int quantity,
            String unit,
            int safetyStock,
            String lastChangedAt
    ) {
    }

    public record InventoryRequest(
            String code,
            @NotBlank(message = "商品编码不能为空") String productCode,
            @Min(value = 0, message = "库存数量不能为负数") int quantity,
            String operator,
            String remark
    ) {
    }
}

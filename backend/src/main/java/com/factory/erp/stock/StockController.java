package com.factory.erp.stock;

import com.factory.erp.common.ApiResponse;
import com.factory.erp.inventory.InventoryController.InventorySummary;
import com.factory.erp.inventory.InventoryService;
import com.factory.erp.inventory.InventoryService.StockActionRequest;
import com.factory.erp.inventory.InventoryService.StockMovement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 入库/出库/库存台账 API。
 * 提供入库、出库操作和库存变动日志查询接口。
 */
@RestController
public class StockController {

    private final InventoryService inventoryService;

    public StockController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/api/stock-in")
    public ApiResponse<InventorySummary> stockIn(@Valid @RequestBody StockAction request) {
        return ApiResponse.ok(inventoryService.stockIn(request.toServiceRequest()));
    }

    @PostMapping("/api/stock-out")
    public ApiResponse<InventorySummary> stockOut(@Valid @RequestBody StockAction request) {
        return ApiResponse.ok(inventoryService.stockOut(request.toServiceRequest()));
    }

    @GetMapping("/api/stock-movements")
    public ApiResponse<List<StockMovement>> movements(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name
    ) {
        return ApiResponse.ok(inventoryService.movements(startDate, endDate, code, name));
    }

    public record StockAction(
            @NotBlank(message = "商品编码不能为空") String productCode,
            @Min(value = 1, message = "数量至少为1") int quantity,
            @NotBlank(message = "经办人不能为空") String operator,
            String remark,
            String customerCode
    ) {
        StockActionRequest toServiceRequest() {
            return new StockActionRequest(productCode, quantity, operator, remark, customerCode);
        }
    }
}

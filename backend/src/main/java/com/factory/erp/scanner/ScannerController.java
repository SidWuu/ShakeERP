package com.factory.erp.scanner;

import com.factory.erp.common.ApiResponse;
import com.factory.erp.common.exception.NotFoundException;
import com.factory.erp.inventory.InventoryService;
import com.factory.erp.inventory.InventoryService.ScannerProduct;
import com.factory.erp.product.ProductService;
import com.factory.erp.product.ProductService.ProductRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 扫码查询 API。
 * 根据商品条码查询商品信息和当前库存数量。
 */
@RestController
@RequestMapping("/api/scanner")
public class ScannerController {

    private final ProductService productService;
    private final InventoryService inventoryService;

    public ScannerController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/products/{barcode}")
    public ApiResponse<ScannerProduct> findProduct(@PathVariable String barcode) {
        ProductRecord product = productService.findByBarcode(barcode);
        if (product == null) {
            throw new NotFoundException("未找到条码对应商品");
        }
        ScannerProduct result = new ScannerProduct(
                product.code(),
                product.barcode(),
                product.name(),
                product.category(),
                product.unit(),
                product.safetyStock(),
                inventoryService.getStock(product.code())
        );
        return ApiResponse.ok(result);
    }
}

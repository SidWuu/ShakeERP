package com.factory.erp.common;

import com.factory.erp.auth.AuthService;
import com.factory.erp.customer.CustomerService;
import com.factory.erp.inventory.InventoryService;
import com.factory.erp.product.ProductService;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;

@Component
@DependsOnDatabaseInitialization
public class StartupDataLoader {

    private final ProductService productService;
    private final CustomerService customerService;
    private final InventoryService inventoryService;
    private final AuthService authService;

    public StartupDataLoader(
            ProductService productService,
            CustomerService customerService,
            InventoryService inventoryService,
            AuthService authService
    ) {
        this.productService = productService;
        this.customerService = customerService;
        this.inventoryService = inventoryService;
        this.authService = authService;
    }

    @PostConstruct
    void load() {
        productService.reloadFromDatabase();
        customerService.reloadFromDatabase();
        inventoryService.reloadFromDatabase();
        authService.reloadFromDatabase();
    }
}

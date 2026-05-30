package com.factory.erp.customer;

import com.factory.erp.common.ApiResponse;
import jakarta.validation.Valid;
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
 * 客户管理 API。
 * 提供客户的新增、查询、更新、删除接口。
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiResponse<List<CustomerSummary>> listCustomers(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name
    ) {
        return ApiResponse.ok(customerService.list(startDate, endDate, code, name));
    }

    @PostMapping
    public ApiResponse<CustomerSummary> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok(customerService.create(request));
    }

    @PutMapping("/{code}")
    public ApiResponse<CustomerSummary> updateCustomer(
            @PathVariable String code,
            @Valid @RequestBody CustomerRequest request
    ) {
        return ApiResponse.ok(customerService.update(code, request));
    }

    @DeleteMapping("/{code}")
    public ApiResponse<CustomerSummary> deleteCustomer(@PathVariable String code) {
        return ApiResponse.ok(customerService.delete(code));
    }

    public record CustomerSummary(
            String code,
            String name,
            String contactPerson,
            String phone,
            String qq,
            String wechat,
            String address,
            String remark,
            String createdAt
    ) {
    }

    public record CustomerRequest(
            String code,
            @NotBlank(message = "客户名称不能为空") String name,
            String contactPerson,
            @NotBlank(message = "联系电话不能为空") String phone,
            String qq,
            String wechat,
            String address,
            String remark
    ) {
    }
}

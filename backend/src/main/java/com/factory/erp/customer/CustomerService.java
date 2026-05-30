package com.factory.erp.customer;

import com.factory.erp.common.BaseService;
import com.factory.erp.common.exception.NotFoundException;
import com.factory.erp.customer.CustomerController.CustomerRequest;
import com.factory.erp.customer.CustomerController.CustomerSummary;
import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 客户管理 Service。
 * 负责客户的 CRUD 操作，数据持久化到 SQLite customers 表。
 */
@Service
public class CustomerService extends BaseService {

    private final Map<String, CustomerRecord> customers = new LinkedHashMap<>();

    public CustomerService(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @PostConstruct
    void init() {
        createSchema();
        loadFromDatabase();
    }

    public synchronized List<CustomerSummary> list(String startDate, String endDate, String code, String name) {
        return customers.values().stream()
                .filter(c -> matchesDate(c.createdAt(), startDate, endDate))
                .filter(c -> contains(c.code(), code))
                .filter(c -> contains(c.name(), name))
                .map(CustomerRecord::toSummary)
                .toList();
    }

    public synchronized CustomerSummary create(CustomerRequest request) {
        CustomerRecord customer = new CustomerRecord(
                nextCode("C", customers, "customers"),
                request.name(),
                request.contactPerson(),
                request.phone(),
                request.qq(),
                request.wechat(),
                request.address(),
                request.remark(),
                now()
        );
        customers.put(customer.code(), customer);
        insertCustomer(customer);
        return customer.toSummary();
    }

    public synchronized CustomerSummary update(String customerCode, CustomerRequest request) {
        CustomerRecord current = customers.get(customerCode);
        if (current == null) {
            throw new NotFoundException("客户不存在");
        }

        CustomerRecord updated = new CustomerRecord(
                current.code(),
                request.name(),
                request.contactPerson(),
                request.phone(),
                request.qq(),
                request.wechat(),
                request.address(),
                request.remark(),
                current.createdAt()
        );
        customers.put(updated.code(), updated);
        updateCustomerRecord(updated);
        return updated.toSummary();
    }

    public synchronized CustomerSummary delete(String customerCode) {
        CustomerRecord customer = customers.remove(customerCode);
        if (customer == null) {
            throw new NotFoundException("客户不存在");
        }
        deleteCustomerRecord(customerCode);
        return customer.toSummary();
    }

    private void createSchema() {
        jdbcTemplate.execute("""
                create table if not exists customers (
                    code text primary key,
                    name text not null,
                    contact_person text,
                    phone text not null,
                    qq text,
                    wechat text,
                    address text,
                    remark text,
                    created_at text not null
                )
                """);
    }

    private void loadFromDatabase() {
        customers.clear();
        jdbcTemplate.query("select code, name, contact_person, phone, qq, wechat, address, remark, created_at from customers order by code", (java.sql.ResultSet rs) -> {
            while (rs.next()) {
                CustomerRecord customer = new CustomerRecord(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("contact_person"),
                        rs.getString("phone"),
                        rs.getString("qq"),
                        rs.getString("wechat"),
                        rs.getString("address"),
                        rs.getString("remark"),
                        rs.getString("created_at")
                );
                customers.put(customer.code(), customer);
            }
        });
    }

    private void insertCustomer(CustomerRecord customer) {
        jdbcTemplate.update(
                "insert into customers (code, name, contact_person, phone, qq, wechat, address, remark, created_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                customer.code(), customer.name(), customer.contactPerson(), customer.phone(),
                customer.qq(), customer.wechat(), customer.address(), customer.remark(), customer.createdAt()
        );
    }

    private void updateCustomerRecord(CustomerRecord customer) {
        jdbcTemplate.update(
                "update customers set name = ?, contact_person = ?, phone = ?, qq = ?, wechat = ?, address = ?, remark = ? where code = ?",
                customer.name(), customer.contactPerson(), customer.phone(),
                customer.qq(), customer.wechat(), customer.address(), customer.remark(), customer.code()
        );
    }

    private void deleteCustomerRecord(String customerCode) {
        jdbcTemplate.update("delete from customers where code = ?", customerCode);
    }

    private record CustomerRecord(
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
        CustomerSummary toSummary() {
            return new CustomerSummary(code, name, contactPerson, phone, qq, wechat, address, remark, createdAt);
        }
    }
}

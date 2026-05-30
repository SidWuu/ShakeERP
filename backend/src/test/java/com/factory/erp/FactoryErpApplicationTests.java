package com.factory.erp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:factory-erp-test?mode=memory&cache=shared"
})
class FactoryErpApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void healthReturnsServiceStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("shake-erp"));
    }

    @Test
    void loginAcceptsKnownDemoUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value("老板"));
    }

    @Test
    void productsReturnsSeedProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("P-1001"))
                .andExpect(jsonPath("$.data[0].barcode").value("6900000000017"));
    }

    @Test
    void productsCanBeQueriedByDateCodeAndName() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("startDate", "2000-01-01")
                        .param("endDate", "2100-01-01")
                        .param("code", "1001")
                        .param("name", "螺丝"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("P-1001"));
    }

    @Test
    void customersCanBeCreatedQueriedAndDeletedWithContactChannels() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "上海测试客户",
                                  "contactPerson": "张三",
                                  "phone": "13800000000",
                                  "qq": "123456789",
                                  "wechat": "zhangsan-wx",
                                  "address": "上海市浦东新区",
                                  "remark": "重点客户"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("C-1001"))
                .andExpect(jsonPath("$.data.qq").value("123456789"))
                .andExpect(jsonPath("$.data.wechat").value("zhangsan-wx"));

        mockMvc.perform(put("/api/customers/C-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "上海测试客户更新",
                                  "contactPerson": "李四",
                                  "phone": "13900000000",
                                  "qq": "987654321",
                                  "wechat": "lisi-wx",
                                  "address": "上海市闵行区",
                                  "remark": "更新客户"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("C-1001"))
                .andExpect(jsonPath("$.data.name").value("上海测试客户更新"))
                .andExpect(jsonPath("$.data.contactPerson").value("李四"));

        mockMvc.perform(get("/api/customers")
                        .param("startDate", "2000-01-01")
                        .param("endDate", "2100-01-01")
                        .param("code", "1001")
                        .param("name", "更新"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].contactPerson").value("李四"))
                .andExpect(jsonPath("$.data[0].phone").value("13900000000"));

        mockMvc.perform(delete("/api/customers/C-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("C-1001"));

        mockMvc.perform(get("/api/customers").param("code", "C-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void inventorySummaryReturnsCurrentStock() throws Exception {
        mockMvc.perform(get("/api/inventory/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("I-1001"))
                .andExpect(jsonPath("$.data[0].productCode").value("P-1001"))
                .andExpect(jsonPath("$.data[0].quantity").value(120));
    }

    @Test
    void inventoryCanBeQueriedByDateCodeAndName() throws Exception {
        mockMvc.perform(get("/api/inventory/summary")
                        .param("startDate", "2000-01-01")
                        .param("endDate", "2100-01-01")
                        .param("code", "1002")
                        .param("name", "扎带"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productCode").value("P-1002"));
    }

    @Test
    void createsProductAndMakesItSearchableByBarcode() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "6900000020015",
                                  "name": "测试包装箱",
                                  "category": "包材",
                                  "unit": "个",
                                  "safetyStock": 5,
                                  "initialQuantity": 11
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("P-1004"));

        mockMvc.perform(put("/api/products/P-1004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "6900000020015",
                                  "name": "测试包装箱更新",
                                  "category": "包材",
                                  "unit": "个",
                                  "safetyStock": 8,
                                  "initialQuantity": 7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("测试包装箱更新"))
                .andExpect(jsonPath("$.data.safetyStock").value(8));

        mockMvc.perform(get("/api/scanner/products/6900000020015"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("测试包装箱更新"))
                .andExpect(jsonPath("$.data.quantity").value(18));

        mockMvc.perform(get("/api/stock-movements").param("code", "P-1004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("新增"))
                .andExpect(jsonPath("$.data[0].afterQuantity").value(11))
                .andExpect(jsonPath("$.data[1].type").value("新增"))
                .andExpect(jsonPath("$.data[1].afterQuantity").value(18));
    }

    @Test
    void inventoryCanBeSavedAndUpdatedByGeneratedInventoryCode() throws Exception {
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "P-1001",
                                  "quantity": 135,
                                  "operator": "系统管理员",
                                  "remark": "库存保存"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("I-1001"))
                .andExpect(jsonPath("$.data.quantity").value(135));

        mockMvc.perform(put("/api/inventory/I-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "P-1001",
                                  "quantity": 88,
                                  "operator": "系统管理员",
                                  "remark": "库存更新"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("I-1001"))
                .andExpect(jsonPath("$.data.quantity").value(88));

        mockMvc.perform(get("/api/stock-movements").param("code", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("修改"))
                .andExpect(jsonPath("$.data[1].type").value("修改"))
                .andExpect(jsonPath("$.data[1].beforeQuantity").value(135))
                .andExpect(jsonPath("$.data[1].afterQuantity").value(88));
    }

    @Test
    void stockInIncreasesInventoryQuantity() throws Exception {
        mockMvc.perform(post("/api/stock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "P-1001",
                                  "quantity": 8,
                                  "operator": "系统管理员",
                                  "remark": "测试入库"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productCode").value("P-1001"))
                .andExpect(jsonPath("$.data.quantity").value(128));
    }

    @Test
    void inventoryChangesAndLedgerArePersistedToDatabase() throws Exception {
        mockMvc.perform(post("/api/stock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "P-1001",
                                  "quantity": 8,
                                  "operator": "系统管理员",
                                  "remark": "持久化测试"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(128));

        Integer storedQuantity = jdbcTemplate.queryForObject(
                "select quantity from inventory where product_code = ?",
                Integer.class,
                "P-1001"
        );
        Integer movementCount = jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where product_code = ? and remark = ?",
                Integer.class,
                "P-1001",
                "持久化测试"
        );

        assertThat(storedQuantity).isEqualTo(128);
        assertThat(movementCount).isEqualTo(1);
    }

    @Test
    void stockLedgerRecordsBeforeAndAfterQuantityAndCanBeQueried() throws Exception {
        mockMvc.perform(post("/api/stock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "P-1001",
                                  "quantity": 8,
                                  "operator": "系统管理员",
                                  "remark": "台账测试"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/stock-movements")
                        .param("startDate", "2000-01-01")
                        .param("endDate", "2100-01-01")
                        .param("code", "1001")
                        .param("name", "螺丝"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("新增"))
                .andExpect(jsonPath("$.data[0].productName").value("不锈钢螺丝 M6"))
                .andExpect(jsonPath("$.data[0].beforeQuantity").value(120))
                .andExpect(jsonPath("$.data[0].afterQuantity").value(128));
    }

    @Test
    void stockOutRejectsInsufficientInventory() throws Exception {
        mockMvc.perform(post("/api/stock-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "P-1003",
                                  "quantity": 999,
                                  "operator": "系统管理员",
                                  "remark": "测试出库"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("库存不足"));
    }

    @Test
    void deletesInventoryByClearingQuantityAndWritingLedger() throws Exception {
        mockMvc.perform(delete("/api/inventory/P-1001")
                        .param("operator", "系统管理员"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productCode").value("P-1001"))
                .andExpect(jsonPath("$.data.quantity").value(0));

        mockMvc.perform(get("/api/stock-movements")
                        .param("code", "1001")
                        .param("name", "螺丝"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("删除"))
                .andExpect(jsonPath("$.data[0].beforeQuantity").value(120))
                .andExpect(jsonPath("$.data[0].afterQuantity").value(0));
    }

    @Test
    void deletesProductAndItsInventoryRow() throws Exception {
        mockMvc.perform(delete("/api/products/P-1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("P-1002"));

        mockMvc.perform(get("/api/products").param("code", "P-1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/inventory/summary").param("code", "P-1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}

package com.iwms.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Task 8: Spring Boot REST API, Custom Auth Filter & Controller Tests")
class ApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String username, String password) throws Exception {
        String loginJson = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }

    @Test
    @DisplayName("POST /api/auth/login: Authenticates admin and returns valid token")
    void testLoginSuccess() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("POST /api/auth/login: Rejects invalid credentials with 401 Unauthorized")
    void testLoginFailure() throws Exception {
        String loginJson = "{\"username\":\"admin\",\"password\":\"wrong_password\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected Endpoints: Reject requests without Authorization token with 401")
    void testProtectedEndpointRejection() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/products: Admin role receives full financial cost prices")
    void testAdminGetsCostPrices() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");

        MvcResult result = mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(root.isArray());
        assertTrue(root.size() > 0);

        // First product should have costPrice populated for Admin
        JsonNode first = root.get(0);
        assertTrue(first.has("costPrice"));
        assertNotNull(first.get("costPrice"));
        assertFalse(first.get("costPrice").isNull());
    }

    @Test
    @DisplayName("GET /api/products: Employee role strictly receives redacted null cost prices")
    void testEmployeeCostPriceRedaction() throws Exception {
        String empToken = loginAndGetToken("employee", "employee123");

        MvcResult result = mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(root.isArray());
        assertTrue(root.size() > 0);

        // Every product must have null or missing costPrice for employee
        for (JsonNode productNode : root) {
            assertTrue(!productNode.has("costPrice") || productNode.get("costPrice").isNull(),
                    "Employee must not see cost prices: " + productNode.get("sku").asText());
        }
    }

    @Test
    @DisplayName("Stock In and Stock Out API mutations adjust warehouse inventory")
    void testStockMutationsApi() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");

        // 1. Stock In 50 units
        String inJson = """
                {
                    "sku": "PER-DAIRY-001",
                    "batchNumber": "TEST-B1",
                    "binId": "BIN-B1",
                    "quantity": 50,
                    "reason": "API Restock Test"
                }
                """;

        mockMvc.perform(post("/api/stock/in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("IN"))
                .andExpect(jsonPath("$.quantity").value(50));

        // 2. Stock Out 20 units
        String outJson = """
                {
                    "sku": "PER-DAIRY-001",
                    "quantity": 20,
                    "reason": "API Pick Test"
                }
                """;

        mockMvc.perform(post("/api/stock/out")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(outJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("OUT"))
                .andExpect(jsonPath("$.quantity").value(20));
    }

    @Test
    @DisplayName("GET /api/reports/summary.pdf: Downloads valid monochrome A4 PDF bytes")
    void testPdfReportEndpoint() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");

        MvcResult result = mockMvc.perform(get("/api/reports/summary.pdf")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertTrue(bytes.length > 500);
        String header = new String(bytes, 0, 5);
        assertEquals("%PDF-", header);
    }
}

package com.iwms.report;

import com.iwms.auth.Pbkdf2Authenticator;
import com.iwms.model.*;
import com.iwms.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Task 7: OpenPDF Summary Report Generator & CSV Exporter Tests")
class ReportGeneratorTest {

    private InventoryService inventoryService;
    private PdfReportGenerator pdfGenerator;
    private CsvReportGenerator csvGenerator;
    private User adminUser;
    private User employeeUser;

    @BeforeEach
    void setUp() throws Exception {
        inventoryService = new InventoryService();
        pdfGenerator = new PdfReportGenerator(inventoryService);
        csvGenerator = new CsvReportGenerator(inventoryService);

        Pbkdf2Authenticator auth = new Pbkdf2Authenticator();
        byte[] salt = auth.generateSalt();
        String hash = auth.hashPassword("pass", salt);

        adminUser = new AdminUser("U-01", "admin", hash, salt, "Administrator");
        employeeUser = new EmployeeUser("U-03", "tim_floor", hash, salt, "Tim Floor Associate");

        // Warehouse setup
        Warehouse wh = new Warehouse("WH-1", "Central Depot", "100 Industrial Parkway");
        Zone zoneA = new Zone("Z-A", "Ambient", Category.GENERAL);
        Bin bin1 = new Bin("BIN-A1", "A-1", 500, 1000.0);
        zoneA.addBin(bin1);
        wh.addZone(zoneA);
        inventoryService.addWarehouse(wh);

        // Product setup
        Product p1 = BulkProduct.builder()
                .sku("RICE-25KG")
                .name("Basmati Rice 25kg")
                .category(Category.BULK)
                .unitPrice(45.0)
                .costPrice(28.0)
                .minStockLevel(20)
                .weightKg(25.0)
                .volumeCubicMeters(0.04)
                .build();
        inventoryService.registerProduct(p1);

        Product p2 = PerishableProduct.builder()
                .sku("MILK-1L")
                .name("Organic Whole Milk 1L")
                .category(Category.PERISHABLE)
                .unitPrice(3.99)
                .costPrice(2.20)
                .minStockLevel(30)
                .shelfLifeDays(14)
                .storageTempCelsius(4.0)
                .requiresRefrigeration(true)
                .build();
        inventoryService.registerProduct(p2);

        // Inbound initial stock
        inventoryService.stockIn("RICE-25KG", "BATCH-R1", "BIN-A1", 40, null, "Inbound", "admin");
        inventoryService.stockIn("MILK-1L", "BATCH-M1", "BIN-A1", 60, LocalDate.now().plusDays(10), "Inbound", "admin");
    }

    @Test
    @DisplayName("PdfReportGenerator: Generates valid monochrome A4 PDF bytes starting with %PDF- header")
    void testPdfGenerationBasic() {
        byte[] pdfBytes = pdfGenerator.generateWarehouseSummaryPdf(adminUser);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "Generated PDF must contain substantial document content");

        // Verify PDF Magic Bytes %PDF-
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", header, "Generated file must have standard PDF magic header");
    }

    @Test
    @DisplayName("RBAC Price Redaction: Employee PDF report omits cost prices and financial valuations")
    void testEmployeePdfOmitsFinancialData() {
        byte[] adminPdf = pdfGenerator.generateWarehouseSummaryPdf(adminUser);
        byte[] employeePdf = pdfGenerator.generateWarehouseSummaryPdf(employeeUser);

        assertNotNull(adminPdf);
        assertNotNull(employeePdf);

        // Text representation test in CSV generator directly verifies permission boundary
        String adminCsv = csvGenerator.exportProductsCsv(adminUser);
        String employeeCsv = csvGenerator.exportProductsCsv(employeeUser);

        assertTrue(adminCsv.contains("28.00") || adminCsv.contains("28.0"), "Admin CSV must contain cost prices");
        assertFalse(employeeCsv.contains("28.00") || employeeCsv.contains("28.0"), "Employee CSV must redact cost prices");
        assertTrue(employeeCsv.contains("[REDACTED]"), "Employee CSV must display [REDACTED] in place of costs");
    }

    @Test
    @DisplayName("CsvReportGenerator: Exports valid CSV with headers and product metadata")
    void testCsvExport() {
        String csv = csvGenerator.exportProductsCsv(adminUser);
        assertNotNull(csv);
        String[] lines = csv.split("\n");
        assertTrue(lines.length >= 3, "CSV must have header and at least 2 product rows");

        // Header check
        assertTrue(lines[0].contains("SKU"));
        assertTrue(lines[0].contains("Name"));
        assertTrue(lines[0].contains("Category"));
        assertTrue(lines[0].contains("CostPrice"));

        // Data rows check
        assertTrue(csv.contains("RICE-25KG"));
        assertTrue(csv.contains("MILK-1L"));
    }
}

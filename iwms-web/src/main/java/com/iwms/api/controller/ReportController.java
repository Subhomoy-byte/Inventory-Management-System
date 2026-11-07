package com.iwms.api.controller;

import com.iwms.api.filter.CustomAuthFilter;
import com.iwms.model.User;
import com.iwms.report.CsvReportGenerator;
import com.iwms.report.PdfReportGenerator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final PdfReportGenerator pdfReportGenerator;
    private final CsvReportGenerator csvReportGenerator;

    public ReportController(PdfReportGenerator pdfReportGenerator,
                            CsvReportGenerator csvReportGenerator) {
        this.pdfReportGenerator = pdfReportGenerator;
        this.csvReportGenerator = csvReportGenerator;
    }

    @GetMapping("/summary.pdf")
    public ResponseEntity<byte[]> downloadSummaryPdf(HttpServletRequest request) {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        byte[] pdfBytes = pdfReportGenerator.generateWarehouseSummaryPdf(user);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"warehouse_floor_summary.pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/products.csv")
    public ResponseEntity<String> downloadProductsCsv(HttpServletRequest request) {
        User user = (User) request.getAttribute(CustomAuthFilter.AUTH_USER_ATTR);
        String csv = csvReportGenerator.exportProductsCsv(user);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory_catalog.csv\"")
                .body(csv);
    }
}

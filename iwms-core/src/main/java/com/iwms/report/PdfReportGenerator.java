package com.iwms.report;

import com.iwms.model.Permission;
import com.iwms.model.Product;
import com.iwms.model.User;
import com.iwms.service.InventoryService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Generates high-contrast monochrome A4 PDF reports for warehouse physical cycle counts.
 * Optimized for low-cost thermal, dot-matrix, and monochrome laser printers on the floor.
 * Automatically enforces RBAC by omitting financial valuations and cost prices for Employee role.
 */
public class PdfReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneId.of("UTC"));

    private final InventoryService inventoryService;

    public PdfReportGenerator(InventoryService inventoryService) {
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService cannot be null");
    }

    @Override
    public byte[] generateWarehouseSummaryPdf(User requestingUser) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        generateWarehouseSummary(baos, requestingUser);
        return baos.toByteArray();
    }

    @Override
    public void generateWarehouseSummary(OutputStream outputStream, User requestingUser) {
        Objects.requireNonNull(outputStream, "Output stream cannot be null");
        Objects.requireNonNull(requestingUser, "Requesting user cannot be null");

        boolean canViewCosts = requestingUser.hasPermission(Permission.VIEW_COST_PRICES);
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Typography (Monochrome Palette)
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Font.BOLD, Color.BLACK);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.BOLD, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, Color.BLACK);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.BLACK);
            Font confidentialFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, Color.BLACK);

            // Document Header
            Paragraph title = new Paragraph("INVENTORY & WAREHOUSE MANAGEMENT SYSTEM (IWMS)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("PHYSICAL CYCLE COUNT SHEET & AUDIT RECONCILIATION", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(10f);
            document.add(subtitle);

            // Metadata Box
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(12f);

            metaTable.addCell(createMetaCell("Generated At: " + DATE_FORMATTER.format(Instant.now()), cellFont));
            metaTable.addCell(createMetaCell("Requested By: " + requestingUser.getUsername() + " (" + requestingUser.getRole().name() + ")", cellFont));
            metaTable.addCell(createMetaCell("Warehouse Facility: WH-MAIN Central Hub", cellFont));
            String accessLevel = canViewCosts ? "CONFIDENTIAL - MANAGEMENT FINANCIAL ACCESS" : "WAREHOUSE FLOOR - INVENTORY AUDIT ONLY";
            metaTable.addCell(createMetaCell("Access Level: " + accessLevel, confidentialFont));

            document.add(metaTable);

            // Operational Summary
            List<Product> products = inventoryService.getAllProducts();
            int totalUnits = products.stream().mapToInt(p -> inventoryService.getCurrentStock(p.getSku())).sum();

            Paragraph summaryHeader = new Paragraph("1. FACILITY SUMMARY", sectionFont);
            summaryHeader.setSpacingAfter(6f);
            document.add(summaryHeader);

            PdfPTable summaryTable = new PdfPTable(canViewCosts ? 3 : 2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(12f);

            summaryTable.addCell(createHeaderCell("Total Catalog SKUs", headerFont));
            summaryTable.addCell(createHeaderCell("Total Stock Units", headerFont));
            if (canViewCosts) {
                summaryTable.addCell(createHeaderCell("Total Inventory Valuation", headerFont));
            }

            summaryTable.addCell(createDataCell(String.valueOf(products.size()), cellFont, Element.ALIGN_CENTER));
            summaryTable.addCell(createDataCell(String.valueOf(totalUnits), cellFont, Element.ALIGN_CENTER));
            if (canViewCosts) {
                double totalValuation = products.stream()
                        .mapToDouble(p -> inventoryService.getCurrentStock(p.getSku()) * p.getCostPrice())
                        .sum();
                summaryTable.addCell(createDataCell(String.format(Locale.US, "$%,.2f", totalValuation), cellFont, Element.ALIGN_RIGHT));
            }

            document.add(summaryTable);

            // Physical Count Checklist Table
            Paragraph checklistHeader = new Paragraph("2. PHYSICAL COUNT CHECKLIST", sectionFont);
            checklistHeader.setSpacingAfter(6f);
            document.add(checklistHeader);

            int columns = canViewCosts ? 7 : 6;
            PdfPTable table = new PdfPTable(columns);
            table.setWidthPercentage(100);
            if (canViewCosts) {
                table.setWidths(new float[]{2.0f, 3.5f, 1.8f, 1.5f, 1.2f, 1.5f, 2.5f});
            } else {
                table.setWidths(new float[]{2.2f, 4.0f, 2.0f, 1.3f, 1.5f, 3.0f});
            }
            table.setSpacingAfter(20f);

            // Headers
            table.addCell(createHeaderCell("SKU", headerFont));
            table.addCell(createHeaderCell("Product Name", headerFont));
            table.addCell(createHeaderCell("Category", headerFont));
            if (canViewCosts) {
                table.addCell(createHeaderCell("Cost ($)", headerFont));
            }
            table.addCell(createHeaderCell("System", headerFont));
            table.addCell(createHeaderCell("Count", headerFont));
            table.addCell(createHeaderCell("Variance / Notes", headerFont));

            // Data Rows
            boolean alt = false;
            for (Product p : products) {
                Color rowBg = alt ? new Color(245, 245, 245) : Color.WHITE;
                alt = !alt;

                table.addCell(createRowCell(p.getSku(), cellFont, Element.ALIGN_LEFT, rowBg));
                table.addCell(createRowCell(p.getName(), cellFont, Element.ALIGN_LEFT, rowBg));
                table.addCell(createRowCell(p.getCategory().name(), cellFont, Element.ALIGN_CENTER, rowBg));
                if (canViewCosts) {
                    table.addCell(createRowCell(String.format(Locale.US, "%.2f", p.getCostPrice()), cellFont, Element.ALIGN_RIGHT, rowBg));
                }
                int stock = inventoryService.getCurrentStock(p.getSku());
                table.addCell(createRowCell(String.valueOf(stock), cellFont, Element.ALIGN_CENTER, rowBg));
                table.addCell(createRowCell("[     ]", cellFont, Element.ALIGN_CENTER, rowBg));
                table.addCell(createRowCell("", cellFont, Element.ALIGN_LEFT, rowBg));
            }

            document.add(table);

            // Sign-off Footer Block
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.addCell(createSignCell("Counted By (Signature): _____________________________\nDate: ____________________", cellFont));
            signTable.addCell(createSignCell("Supervisor Verification: _____________________________\nDate: ____________________", cellFont));
            document.add(signTable);

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF document", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private PdfPCell createHeaderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(230, 230, 230));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4f);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private PdfPCell createDataCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4f);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private PdfPCell createRowCell(String text, Font font, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(3f);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private PdfPCell createMetaCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2f);
        return cell;
    }

    private PdfPCell createSignCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(15f);
        return cell;
    }
}

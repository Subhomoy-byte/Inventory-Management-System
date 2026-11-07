package com.iwms.report;

import com.iwms.model.User;

import java.io.OutputStream;

/**
 * Contract for generating warehouse floor audit reports and summary exports.
 */
public interface ReportGenerator {

    void generateWarehouseSummary(OutputStream outputStream, User requestingUser);

    byte[] generateWarehouseSummaryPdf(User requestingUser);
}

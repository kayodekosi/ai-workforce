package com.knatware.aiworkforce.service;

import com.knatware.aiworkforce.model.*;
import com.knatware.aiworkforce.repository.DepartmentRepository;
import com.knatware.aiworkforce.repository.StaffRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Bulk staff enrolment from an Excel (.xlsx) sheet, plus generation of a sample
 * template for download. The expected columns (row 1 = headers) are:
 *
 *   Full Name | Email | Type (AI/HUMAN) | Position | Function | Department |
 *   Phone Ext | Reports To (email) | System Prompt | Model | Connector
 *
 * Rows are validated; problems are collected and returned rather than aborting
 * the whole import, so a partial sheet still enrols the valid rows.
 */
@Service
public class StaffImportService {

    private final StaffRepository staff;
    private final DepartmentRepository departments;
    private final AuditService audit;

    public StaffImportService(StaffRepository staff, DepartmentRepository departments, AuditService audit) {
        this.staff = staff;
        this.departments = departments;
        this.audit = audit;
    }

    private static final String[] HEADERS = {
        "Full Name", "Email", "Type (AI/HUMAN)", "Position", "Function",
        "Department", "Phone Ext", "Reports To (email)", "System Prompt", "Model", "Connector"
    };

    public record ImportResult(int created, List<String> errors) { }

    public ImportResult importFrom(MultipartFile file) throws Exception {
        List<String> errors = new ArrayList<>();
        int created = 0;

        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            int firstRow = sheet.getFirstRowNum();
            for (int r = firstRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String name = cell(row, 0);
                if (name == null || name.isBlank()) continue; // skip empty lines

                try {
                    Staff s = new Staff();
                    s.setFullName(name);
                    s.setEmail(cell(row, 1));
                    String type = cell(row, 2);
                    s.setType("HUMAN".equalsIgnoreCase(type) ? StaffType.HUMAN : StaffType.AI);
                    s.setPosition(cell(row, 3));
                    s.setFunction(cell(row, 4));
                    String deptName = cell(row, 5);
                    if (deptName != null && !deptName.isBlank()) {
                        Department d = departments.findAll().stream()
                                .filter(x -> x.getName().equalsIgnoreCase(deptName.trim()))
                                .findFirst()
                                .orElseGet(() -> departments.save(new Department(deptName.trim())));
                        s.setDepartment(d);
                    }
                    s.setPhoneExtension(cell(row, 6));
                    String reportsToEmail = cell(row, 7);
                    if (reportsToEmail != null && !reportsToEmail.isBlank()) {
                        staff.findAll().stream()
                                .filter(x -> reportsToEmail.equalsIgnoreCase(x.getEmail()))
                                .findFirst().ifPresent(s::setReportsTo);
                    }
                    s.setSystemPrompt(cell(row, 8));
                    s.setModel(cell(row, 9));
                    String connector = cell(row, 10);
                    if (connector != null && !connector.isBlank()) {
                        try { s.setConnector(ConnectorType.valueOf(connector.trim().toUpperCase())); }
                        catch (Exception ex) { s.setConnector(ConnectorType.NONE); }
                    }
                    staff.save(s);
                    created++;
                } catch (Exception rowErr) {
                    errors.add("Row " + (r + 1) + ": " + rowErr.getMessage());
                }
            }
        }
        audit.log(null, "STAFF_IMPORT", "Imported " + created + " staff from Excel"
                + (errors.isEmpty() ? "" : " (" + errors.size() + " errors)"));
        return new ImportResult(created, errors);
    }

    /** Build a sample .xlsx template with headers and two example rows. */
    public byte[] sampleTemplate() throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Staff");

            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont(); bold.setBold(true);
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            String[][] examples = {
                {"Ada Support", "ada@knatware.com", "AI", "Support Lead", "Support",
                 "Customer Support", "1001", "", "You are Ada, a calm support lead.", "gpt-4o", "NONE"},
                {"Jordan Lee", "jordan@knatware.com", "HUMAN", "Operations Manager", "Human Operator",
                 "Sales", "2001", "", "", "", "NONE"}
            };
            for (int r = 0; r < examples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < examples[r].length; c++) row.createCell(c).setCellValue(examples[r][c]);
            }
            for (int i = 0; i < HEADERS.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        }
    }

    private final DataFormatter formatter = new DataFormatter();

    private String cell(Row row, int idx) {
        Cell c = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;
        String v = formatter.formatCellValue(c);
        return v == null ? null : v.trim();
    }
}

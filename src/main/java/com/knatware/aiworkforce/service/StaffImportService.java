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
    private final com.knatware.aiworkforce.repository.LevelRepository levels;
    private final AuditService audit;

    public StaffImportService(StaffRepository staff, DepartmentRepository departments,
                              com.knatware.aiworkforce.repository.LevelRepository levels,
                              AuditService audit) {
        this.staff = staff;
        this.departments = departments;
        this.levels = levels;
        this.audit = audit;
    }

    private static final String[] HEADERS = {
        "ID", "Full Name", "Email", "Type (AI/HUMAN)", "Position", "Function",
        "Department", "Level", "Phone Ext", "Reports To (ID or Email)",
        "System Prompt", "Model", "Connector"
    };

    public record ImportResult(int created, List<String> errors) { }

    public ImportResult importFrom(MultipartFile file) throws Exception {
        List<String> errors = new ArrayList<>();
        int created = 0;

        // Track sheet-ID -> saved staff, and remember each row's reports-to ref
        // so we can resolve reporting lines in a second pass (supports referencing
        // staff defined later in the sheet).
        java.util.Map<String, Staff> bySheetId = new java.util.HashMap<>();
        java.util.List<Object[]> reportLinks = new java.util.ArrayList<>(); // [Staff, reportsToRef]

        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            int firstRow = sheet.getFirstRowNum();
            for (int r = firstRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String sheetId = cell(row, 0);
                String name = cell(row, 1);
                if (name == null || name.isBlank()) continue; // skip empty lines

                try {
                    Staff s = new Staff();
                    s.setFullName(name);
                    s.setEmail(cell(row, 2));
                    String type = cell(row, 3);
                    s.setType("HUMAN".equalsIgnoreCase(type) ? StaffType.HUMAN : StaffType.AI);
                    s.setPosition(cell(row, 4));
                    s.setFunction(cell(row, 5));
                    String deptName = cell(row, 6);
                    if (deptName != null && !deptName.isBlank()) {
                        Department d = departments.findAll().stream()
                                .filter(x -> x.getName().equalsIgnoreCase(deptName.trim()))
                                .findFirst()
                                .orElseGet(() -> departments.save(new Department(deptName.trim())));
                        s.setDepartment(d);
                    }
                    String levelName = cell(row, 7);
                    if (levelName != null && !levelName.isBlank()) {
                        final String ln = levelName.trim();
                        Level lvl = levels.findAll().stream()
                                .filter(x -> x.getName().equalsIgnoreCase(ln))
                                .findFirst()
                                .orElseGet(() -> {
                                    int nextRank = levels.findAll().stream()
                                            .mapToInt(Level::getRank).max().orElse(0) + 1;
                                    return levels.save(new Level(ln, nextRank));
                                });
                        s.setLevel(lvl);
                    }
                    s.setPhoneExtension(cell(row, 8));
                    String reportsToRef = cell(row, 9);   // ID-within-sheet OR email
                    s.setSystemPrompt(cell(row, 10));
                    s.setModel(cell(row, 11));
                    String connector = cell(row, 12);
                    if (connector != null && !connector.isBlank()) {
                        try { s.setConnector(ConnectorType.valueOf(connector.trim().toUpperCase())); }
                        catch (Exception ex) { s.setConnector(ConnectorType.NONE); }
                    }
                    Staff saved = staff.save(s);
                    created++;
                    if (sheetId != null && !sheetId.isBlank()) bySheetId.put(sheetId.trim(), saved);
                    if (reportsToRef != null && !reportsToRef.isBlank()) {
                        reportLinks.add(new Object[]{saved, reportsToRef.trim()});
                    }
                } catch (Exception rowErr) {
                    errors.add("Row " + (r + 1) + ": " + rowErr.getMessage());
                }
            }
        }

        // Second pass: resolve reporting lines (by sheet ID first, then by email).
        for (Object[] link : reportLinks) {
            Staff s = (Staff) link[0];
            String ref = (String) link[1];
            Staff manager = bySheetId.get(ref);
            if (manager == null) {
                manager = staff.findAll().stream()
                        .filter(x -> ref.equalsIgnoreCase(x.getEmail()))
                        .findFirst().orElse(null);
            }
            if (manager != null && !manager.getId().equals(s.getId())) {
                s.setReportsTo(manager);
                staff.save(s);
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
                {"1", "Ada Support", "ada@knatware.com", "AI", "Support Lead", "Support",
                 "Customer Support", "Team Lead", "1001", "", "You are Ada, a calm support lead.", "gpt-4o", "NONE"},
                {"2", "Max Agent", "max@knatware.com", "AI", "Support Agent", "Support",
                 "Customer Support", "Associate", "1002", "1", "You are Max, a friendly tier-1 agent.", "gpt-4o-mini", "NONE"},
                {"3", "Jordan Lee", "jordan@knatware.com", "HUMAN", "Operations Manager", "Human Operator",
                 "Sales", "Manager", "2001", "", "", "", "NONE"}
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

    /** Export all current staff to an .xlsx using the same columns as the template. */
    public byte[] exportAll() throws Exception {
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

            java.util.List<Staff> all = staff.findAll();
            int r = 1;
            for (Staff s : all) {
                Row row = sheet.createRow(r++);
                // columns match HEADERS: ID, Name, Email, Type, Position, Function,
                // Department, Level, Phone Ext, Reports To (ID or Email), System Prompt, Model, Connector
                row.createCell(0).setCellValue(s.getId() == null ? "" : s.getId().toString());
                row.createCell(1).setCellValue(nz(s.getFullName()));
                row.createCell(2).setCellValue(nz(s.getEmail()));
                row.createCell(3).setCellValue(s.getType() == null ? "" : s.getType().name());
                row.createCell(4).setCellValue(nz(s.getPosition()));
                row.createCell(5).setCellValue(nz(s.getFunction()));
                row.createCell(6).setCellValue(s.getDepartment() == null ? "" : nz(s.getDepartment().getName()));
                row.createCell(7).setCellValue(s.getLevel() == null ? "" : nz(s.getLevel().getName()));
                row.createCell(8).setCellValue(nz(s.getPhoneExtension()));
                // export reports-to as the manager's ID (re-importable)
                row.createCell(9).setCellValue(s.getReportsTo() == null || s.getReportsTo().getId() == null
                        ? "" : s.getReportsTo().getId().toString());
                row.createCell(10).setCellValue(nz(s.getSystemPrompt()));
                row.createCell(11).setCellValue(nz(s.getModel()));
                row.createCell(12).setCellValue(s.getConnector() == null ? "NONE" : s.getConnector().name());
            }
            for (int i = 0; i < HEADERS.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        }
    }

    private String nz(String v) { return v == null ? "" : v; }

    private final DataFormatter formatter = new DataFormatter();

    private String cell(Row row, int idx) {
        Cell c = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;
        String v = formatter.formatCellValue(c);
        return v == null ? null : v.trim();
    }
}

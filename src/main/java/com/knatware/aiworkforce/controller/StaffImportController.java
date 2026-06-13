package com.knatware.aiworkforce.controller;

import com.knatware.aiworkforce.service.StaffImportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Bulk staff enrolment via Excel upload, plus a downloadable sample template.
 */
@RestController
@RequestMapping("/api/staff-import")
public class StaffImportController {

    private final StaffImportService importer;

    public StaffImportController(StaffImportService importer) { this.importer = importer; }

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            StaffImportService.ImportResult result = importer.importFrom(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "created", 0, "errors", java.util.List.of("Could not read file: " + e.getMessage())));
        }
    }

    @GetMapping("/sample")
    public ResponseEntity<ByteArrayResource> sample() throws Exception {
        byte[] bytes = importer.sampleTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=staff-import-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(bytes));
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> export() throws Exception {
        byte[] bytes = importer.exportAll();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=staff-roster.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(bytes));
    }
}

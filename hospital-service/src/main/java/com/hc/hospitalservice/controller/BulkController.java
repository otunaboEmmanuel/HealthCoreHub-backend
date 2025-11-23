package com.hc.hospitalservice.controller;

import com.hc.hospitalservice.dto.BulkUploadResponse;
import com.hc.hospitalservice.service.BulkUserUploadService;
import com.hc.hospitalservice.service.TemplateDownloadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/users")
@Slf4j
@RequiredArgsConstructor
public class BulkController {
    private final BulkUserUploadService bulkUserUploadService;
    private final TemplateDownloadService templateDownloadService;
    @PostMapping(value = "bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkUpload(@RequestParam("file") MultipartFile file,@RequestHeader(value = "X-Hospital-Id", required = false) String hospitalId,
                                        @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,@RequestHeader("X-Tenant-Role") String tenantRole) {
        try{
            if(!tenantRole.equalsIgnoreCase("admin")){
                log.info("to upload bulk upload files is not accessible to this user-role {}",tenantRole);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You are not allowed to access this endpoint, contact hospital-admin"));
            }
            if(file.isEmpty()){
                log.info("file is empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "file is empty"));
            }
            String fileName = file.getOriginalFilename();
            if (fileName == null||fileName.endsWith(".xlsx") && !fileName.contains(".csv")) {
                log.info("only csv and .xlsx files are supported");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "only csv and .xlsx files are supported"));
            }
            log.info(" Processing bulk upload: {} for hospital: {}", fileName, hospitalId);
             BulkUploadResponse response = bulkUserUploadService.processBulkUpload(file, tenantDb, hospitalId);
             return ResponseEntity.ok().body(response);
        } catch (IOException e) {
            log.error(" Bulk upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Bulk upload failed: " + e.getMessage()));
        }
        }
    @GetMapping("/bulk-upload/template")
    public ResponseEntity<?> downloadTemplate(@RequestParam String format,@RequestHeader("X-Tenant-Role") String tenantRole) {
        try {
            if(!tenantRole.equalsIgnoreCase("admin")){
                log.warn("bulk upload template is not accessible to this user-role {}",tenantRole);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","You are not allowed to access this endpoint"));
            }
            if ("csv".equalsIgnoreCase(format)) {
                ByteArrayResource resource = templateDownloadService.generateCSVTemplate();

                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=user-upload-template.csv")
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .contentLength(resource.contentLength())
                        .body(resource);

            } else if ("xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format)) {
                ByteArrayResource resource = templateDownloadService.generateExcelTemplate();

                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=user-upload-template.xlsx")
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .contentLength(resource.contentLength())
                        .body(resource);

            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid format. Use 'csv' or 'xlsx'"));
            }

        } catch (Exception e) {
            log.error(" Failed to generate template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate template"));
        }
    }
}


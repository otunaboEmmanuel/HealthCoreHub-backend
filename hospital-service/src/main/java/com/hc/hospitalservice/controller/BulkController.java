package com.hc.hospitalservice.controller;

import com.hc.hospitalservice.service.BulkUserUploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/users")
@Slf4j
@RequiredArgsConstructor
public class BulkController {
    private final BulkUserUploadService bulkUserUploadService;

    @PostMapping(value = "bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkUpload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try{
            String tenantRole = request.getAttribute("tenantRole").toString();
            String tenantDbName = request.getAttribute("tenantDb").toString();
            String hospitalId = request.getAttribute("hospitalId").toString();
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
            Map<String, String> response = bulkUserUploadService.processBulkUpload(file, tenantDbName, hospitalId);
        }
    }
}

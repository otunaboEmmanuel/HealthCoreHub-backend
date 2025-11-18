package com.hc.hospitalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResponse {

    private int totalRecords;
    private int successful;
    private int failed;
    private List<FailureDetail> failures;
    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureDetail {
        private int rowNumber;
        private String email;
        private String reason;
    }
}

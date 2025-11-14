package com.hc.hospitalservice.grpc;
import com.hc.authservice.grpc.AuthServiceGrpc;
import com.hc.authservice.grpc.RegisterUserRequest;
import com.hc.authservice.grpc.RegisterUserResponse;
import com.hc.authservice.grpc.StaffRequest;
import com.hc.authservice.grpc.StaffResponse;
import com.hc.authservice.grpc.DeleteRequest;
import com.hc.authservice.grpc.DeleteResponse;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class AuthServiceGrpcClient {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceStub;

    public String registerUser(String email, String password, Integer hospitalId, String tenantDb, String globalRole) {
        try {
            RegisterUserRequest request = RegisterUserRequest.newBuilder()
                    .setEmail(email)
                    .setPassword(password)
                    .setHospitalId(hospitalId)
                    .setTenantDb(tenantDb)
                    .setGlobalRole(globalRole)
                    .build();

            log.info(" Sending gRPC registration request for {}", email);

            RegisterUserResponse response = authServiceStub.registerUser(request);

            log.info(" gRPC user registered successfully: {}", response.getUserId());

            return response.getUserId(); //  return the ID back to HospitalRegistrationService

        } catch (StatusRuntimeException e) {
            log.error(" gRPC request failed: {}", e.getStatus().getDescription());
            throw new RuntimeException("gRPC registration failed: " + e.getMessage(), e);
        }
    }
    public Map<String, String> registerStaff(String email, Integer hospitalId, String tenantDb, String globalRole) {
        try {
            StaffRequest staffRequest = StaffRequest.newBuilder()
                    .setEmail(email)
                    .setGlobalRole(globalRole)
                    .setHospitalId(hospitalId)
                    .setTenantDb(tenantDb)
                    .build();
            log.info(" Sending gRPC staff request for {}", email);
            StaffResponse staffResponse = authServiceStub.registerStaff(staffRequest);
            log.info(" gRPC staff registered successfully: {}", staffResponse.getUserId());
            Map<String, String> map = new HashMap<>();
            map.put("userId", staffResponse.getUserId());
            map.put("activationCode", staffResponse.getActivationCode());
            return map;
        }catch (StatusRuntimeException e){
            log.error(" gRPC request failed: {}", e.getStatus().getDescription());
            throw new RuntimeException("gRPC staff request failed: " + e.getMessage(), e);
        }
    }
    public void deleteUser(String userId){
        try{
            DeleteRequest request = DeleteRequest.newBuilder().setUserId(userId).build();
            log.info(" Sending gRPC delete request for {}", userId);
             DeleteResponse response = authServiceStub.deleteUser(request);
            log.info("deleting user with id {} successfully", userId );
        }catch (Exception e){
            log.error(" gRPC delete request failed: {}", e.getMessage());
            throw new RuntimeException("delete user failed: " + e.getMessage(), e);
        }
    }
}


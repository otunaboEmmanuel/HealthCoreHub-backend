package com.hc.hospitalservice.grpc;
import com.hc.authservice.grpc.AuthServiceGrpc;
import com.hc.authservice.grpc.RegisterUserRequest;
import com.hc.authservice.grpc.RegisterUserResponse;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;


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
}


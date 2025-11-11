package com.hc.authservice.grpc;

import com.hc.authservice.entity.AuthUser;
import com.hc.authservice.service.AuthService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Map;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {
    private final AuthService authService;
    @Override
    public void registerAdmin(RegisterAdminRequest request, StreamObserver<RegisterAdminResponse> responseObserver) {
        try{
            log.info("registerAdmin start with email {}", request.getEmail());
            AuthUser savedUser = authService.registerUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getHospitalId(),
                    request.getTenantDb(),
                    request.getGlobalRole()
            );
            RegisterAdminResponse adminResponse = RegisterAdminResponse.newBuilder()
                    .setUserId(savedUser.getId().toString())
                    .setMessage("Admin registered successfully")
                    .setSuccess(true)
                    .build();
            responseObserver.onNext(adminResponse);
            responseObserver.onCompleted();
        }catch (Exception ex){
            log.error("registerAdmin error",ex);
            responseObserver.onError(Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
        }
    }
}

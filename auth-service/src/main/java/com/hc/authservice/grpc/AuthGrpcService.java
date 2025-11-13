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
    public void registerUser(RegisterUserRequest request, StreamObserver<RegisterUserResponse> responseObserver) {
        try{
            log.info("registerAdmin start with email {}", request.getEmail());
            AuthUser savedUser = authService.registerUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getHospitalId(),
                    request.getTenantDb(),
                    request.getGlobalRole()
            );
            RegisterUserResponse userResponse = RegisterUserResponse.newBuilder()
                    .setUserId(savedUser.getId().toString())
                    .setMessage("user registered successfully with role "+ savedUser.getGlobalRole())
                    .setSuccess(true)
                    .build();
            responseObserver.onNext(userResponse);
            responseObserver.onCompleted();
        }catch (Exception ex){
            log.error("registerUser error",ex);
            responseObserver.onError(Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void registerStaff(StaffRequest request, StreamObserver<StaffResponse> responseObserver) {
        try{
            log.info("registerStaff start with email {}", request.getEmail());
            AuthUser authUser = authService.registerStaff(
                    request.getEmail(),
                    request.getHospitalId(),
                    request.getTenantDb(),
                    request.getGlobalRole()
            );
            StaffResponse staffResponse = StaffResponse.newBuilder()
                    .setUserId(authUser.getId().toString())
                    .setMessage("user is temporarily registered  "+ authUser.getGlobalRole())
                    .setSuccess(true)
                    .setActivationCode(authUser.getActivation_token())
                    .build();
            responseObserver.onNext(staffResponse);
            responseObserver.onCompleted();
        }catch (Exception ex){
            log.error("registerStaff error",ex);
            responseObserver.onError(Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
        }
    }
}

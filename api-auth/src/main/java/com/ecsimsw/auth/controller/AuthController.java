package com.ecsimsw.auth.controller;

import com.ecsimsw.auth.dto.LogInRequest;
import com.ecsimsw.auth.dto.LogInResponse;
import com.ecsimsw.auth.dto.ReissueRequest;
import com.ecsimsw.auth.dto.Tokens;
import com.ecsimsw.auth.service.AuthService;
import com.ecsimsw.auth.service.CustomUserDetail;
import com.ecsimsw.common.client.dto.AuthCreationRequest;
import com.ecsimsw.common.client.dto.AuthUpdateRequest;
import com.ecsimsw.common.dto.ApiResponse;
import com.ecsimsw.common.support.annotation.InternalHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    @PostMapping("/api/auth/login")
    public ApiResponse<LogInResponse> login(@RequestBody LogInRequest request) {
        var authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        var userDetail = (CustomUserDetail) authentication.getPrincipal();
        var result = authService.issue(userDetail.username());
        return ApiResponse.success(result);
    }

    @PostMapping("/api/auth/reissue")
    public ApiResponse<Tokens> reissue(@RequestBody ReissueRequest request) {
        var result = authService.reissue(request.refreshToken()).tokens();
        return ApiResponse.success(result);
    }

    @PostMapping("/api/auth/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        getToken(request).ifPresent(authService::blockToken);
        return ApiResponse.success();
    }

    @InternalHandler
    @PostMapping("/api/auth/user")
    public ResponseEntity<Void> createUser(@RequestBody AuthCreationRequest request) {
        log.info("Creating user {}", request.username());
        authService.createUserAuth(request);
        return ResponseEntity.ok().build();
    }

    @InternalHandler
    @PutMapping("/api/auth/user")
    public ResponseEntity<Void> updateUser(@RequestBody AuthUpdateRequest request) {
        authService.updateUserAuth(request);
        return ResponseEntity.ok().build();
    }

    private Optional<String> getToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.of(authHeader.substring("Bearer ".length()));
    }
}

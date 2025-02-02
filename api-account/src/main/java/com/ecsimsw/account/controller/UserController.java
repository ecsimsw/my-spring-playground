package com.ecsimsw.account.controller;

import com.ecsimsw.account.dto.SignUpRequest;
import com.ecsimsw.account.dto.SignUpResponse;
import com.ecsimsw.account.dto.UpdatePasswordRequest;
import com.ecsimsw.account.dto.UserInfoResponse;
import com.ecsimsw.account.service.UserService;
import com.ecsimsw.auth.domain.CustomUserDetail;
import com.ecsimsw.auth.service.AuthService;
import com.ecsimsw.auth.utils.TokenUtils;
import com.ecsimsw.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/api/user/signup")
    public ApiResponse<SignUpResponse> user(@RequestBody SignUpRequest request) {
        var id = userService.create(request);
        return ApiResponse.success(new SignUpResponse(id));
    }

    @GetMapping("/api/user/me")
    public ApiResponse<UserInfoResponse> me(@AuthenticationPrincipal UserDetails user) {
        var result = userService.userInfo(user.getUsername());
        return ApiResponse.success(result);
    }

    @GetMapping("/api/user/roles")
    public ApiResponse<List<String>> roles(@AuthenticationPrincipal CustomUserDetail user) {
        var roles = user.roleNames();
        return ApiResponse.success(roles);
    }

    @PutMapping("/api/user/password")
    public ApiResponse<Void> password(@AuthenticationPrincipal UserDetails user, @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(user.getUsername(), request.password());
        return ApiResponse.success();
    }

    @DeleteMapping("/api/user")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
        TokenUtils.getToken(request)
            .ifPresent(authService::blockToken);
        userService.delete(userDetails.getUsername());
        return ApiResponse.success();
    }
}

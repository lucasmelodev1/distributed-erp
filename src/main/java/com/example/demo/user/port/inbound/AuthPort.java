package com.example.demo.user.port.inbound;

import com.example.demo.user.adapter.web.dto.auth.CreateUserByAuthTokenRequest;
import com.example.demo.user.adapter.web.dto.auth.LoginRequest;
import com.example.demo.user.adapter.web.dto.auth.LoginResponse;
import com.example.demo.user.adapter.web.dto.auth.RequestEmailVerificationRequest;
import com.example.demo.user.adapter.web.dto.auth.RequestEmailVerificationResponse;
import com.example.demo.user.adapter.web.dto.auth.RequestResetPasswordRequest;
import com.example.demo.user.adapter.web.dto.auth.RequestResetPasswordResponse;
import com.example.demo.user.adapter.web.dto.auth.ResetPasswordRequest;
import com.example.demo.user.adapter.web.dto.auth.VerifyEmailRequest;
import com.example.demo.user.adapter.web.dto.auth.VerifyEmailResponse;
import com.example.demo.user.adapter.web.dto.auth.VerifyPasswordResetRequestRequest;
import com.example.demo.user.adapter.web.dto.auth.VerifyResetPasswordResponse;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthPort {
    RequestEmailVerificationResponse requestEmailVerification(RequestEmailVerificationRequest dto);

    VerifyEmailResponse verifyEmail(VerifyEmailRequest dto) throws EntityNotFoundException;

    LoginResponse createUserByAuthToken(CreateUserByAuthTokenRequest dto, HttpServletRequest request,
            HttpServletResponse response) throws EntityNotFoundException;

    RequestResetPasswordResponse requestResetPassword(RequestResetPasswordRequest dto);

    VerifyResetPasswordResponse verifyPasswordResetToken(VerifyPasswordResetRequestRequest dto)
            throws EntityNotFoundException;

    void resetPassword(ResetPasswordRequest dto) throws EntityNotFoundException;

    LoginResponse login(LoginRequest dto, HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);
}

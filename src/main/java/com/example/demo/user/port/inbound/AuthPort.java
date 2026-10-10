package com.example.demo.user.port.inbound;

import com.example.demo.user.adapter.web.dto.auth.RegisterUserInput;
import com.example.demo.user.adapter.web.dto.auth.LoginInput;
import com.example.demo.user.adapter.web.dto.auth.LoginPayload;
import com.example.demo.user.adapter.web.dto.auth.RequestEmailVerificationInput;
import com.example.demo.user.adapter.web.dto.auth.RequestEmailVerificationPayload;
import com.example.demo.user.adapter.web.dto.auth.RequestPasswordResetInput;
import com.example.demo.user.adapter.web.dto.auth.RequestPasswordResetPayload;
import com.example.demo.user.adapter.web.dto.auth.ResetPasswordInput;
import com.example.demo.user.adapter.web.dto.auth.VerifyEmailInput;
import com.example.demo.user.adapter.web.dto.auth.VerifyEmailPayload;
import com.example.demo.user.adapter.web.dto.auth.VerifyPasswordResetInput;
import com.example.demo.user.adapter.web.dto.auth.VerifyPasswordResetPayload;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthPort {
    RequestEmailVerificationPayload requestEmailVerification(RequestEmailVerificationInput dto);

    VerifyEmailPayload verifyEmail(VerifyEmailInput dto) throws EntityNotFoundException;

    LoginPayload createUserByAuthToken(RegisterUserInput dto, HttpServletRequest request,
            HttpServletResponse response) throws EntityNotFoundException;

    RequestPasswordResetPayload requestResetPassword(RequestPasswordResetInput dto);

    VerifyPasswordResetPayload verifyPasswordResetToken(VerifyPasswordResetInput dto)
            throws EntityNotFoundException;

    void resetPassword(ResetPasswordInput dto) throws EntityNotFoundException;

    LoginPayload login(LoginInput dto, HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);
}

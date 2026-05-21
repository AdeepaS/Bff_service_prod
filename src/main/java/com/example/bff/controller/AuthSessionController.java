package com.example.bff.controller;

import com.example.bff.DTO.ApiResponse;
import com.example.bff.logger.LoggerAdapter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/BFF/api/proxy")
public class AuthSessionController {

    @Autowired
    LoggerAdapter logger;

    @GetMapping("/auth/session")
    public ResponseEntity<ApiResponse<?>> getSession(HttpServletRequest request) {
        logger.info("Attempting to retrieve session from cookies...");

        try {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                logger.warn("No cookies found.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, HttpStatus.UNAUTHORIZED.value(), "No session found", null));
            }

            String accessToken = null;
            String userName = null;
            String userRoleId = null;
            String WorkFlowRoleId = null;

            for (Cookie cookie : cookies) {
                switch (cookie.getName()) {
                    case "AccessToken", "__Secure-AccessToken" -> {
                        accessToken = cookie.getValue();
                    }
                    case "UserName", "__Secure-UserName" -> {
                        userName = cookie.getValue();
                    }
                    case "RoleId", "__Secure-RoleId" -> {
                        userRoleId = cookie.getValue();
                    }
                    case "workFlowRoleId", "__Secure-workFlowRoleId" -> {
                        WorkFlowRoleId = cookie.getValue();
                    }
                }
            }


            if (accessToken == null) {
                logger.warn("AccessToken cookie missing.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, HttpStatus.UNAUTHORIZED.value(), "Access token missing", null));
            }

            // You may add JWT validation here if needed


            Map<String, Object> user = new HashMap<>();
//            user.put("access_token", accessToken);
            user.put("user_name", userName);
            assert userRoleId != null;
            user.put("user_role_Id" , Long.parseLong(userRoleId));
            assert WorkFlowRoleId != null;
            user.put("work_flow_role_Id" , Long.parseLong(WorkFlowRoleId));

            logger.info("Session retrieved successfully for user: " + userName);
            return ResponseEntity.ok(new ApiResponse<>(true, HttpStatus.OK.value(), "Session found", user));

        } catch (Exception ex) {
            logger.error("Error retrieving session: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error", null));
        }
    }
}

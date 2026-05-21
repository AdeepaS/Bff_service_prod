package com.example.bff.controller;


import com.example.bff.DTO.ApiResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorHandlerController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<?> handleError() {
        return new ResponseEntity<>(
                new ApiResponse<>(false, HttpStatus.NOT_FOUND.value(),
                        "Resource not found", null),
                HttpStatus.NOT_FOUND);
    }

    public String getErrorPath() {
        return "/error";
    }

}

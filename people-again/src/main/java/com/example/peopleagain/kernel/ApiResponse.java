package com.example.peopleagain.kernel;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class ApiResponse {
    private String message;
    private Object data;
    private Boolean error;
    private HttpStatus status;

    public ApiResponse(String message, Object data, Boolean error, HttpStatus status) {
        this.message = message;
        this.data = data;
        this.error = error;
        this.status = status;
    }
}

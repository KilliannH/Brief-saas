package com.killiann.briefsaas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends Throwable {
    public ForbiddenException(String s) {
    }
}

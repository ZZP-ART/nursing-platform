package com.nursing.common.exception;

import org.springframework.http.HttpStatus;
 
public class BusinessException extends RuntimeException {
     private final int code;
     private final HttpStatus status;
 
     public BusinessException(int code, String message) {
         this(code, message, statusForCode(code));
     }

     public BusinessException(int code, String message, HttpStatus status) {
         super(message);
         this.code = code;
         this.status = status == null ? statusForCode(code) : status;
     }
 
     public int getCode() { return code; }

     public HttpStatus getStatus() { return status; }

     private static HttpStatus statusForCode(int code) {
         return switch (code) {
             case 1002, 1003 -> HttpStatus.UNAUTHORIZED;
             case 1004, 3008, 3012, 4007 -> HttpStatus.FORBIDDEN;
             case 1005, 3007, 4006 -> HttpStatus.NOT_FOUND;
             case 1006, 2008, 3002, 4003 -> HttpStatus.CONFLICT;
             case 1007, 2003, 2004, 2009, 2011, 3001, 3003, 3004, 3006, 3009, 3010, 4002 -> HttpStatus.UNPROCESSABLE_ENTITY;
             case 1008, 2001, 2002 -> HttpStatus.TOO_MANY_REQUESTS;
             case 1999 -> HttpStatus.INTERNAL_SERVER_ERROR;
             default -> HttpStatus.BAD_REQUEST;
         };
     }
 }

 package com.nursing.common.constant;
 
 public final class ApiCode {
     // Common: 1000-1999
     public static final int SUCCESS = 0;
     public static final int PARAM_ERROR = 1000;
     public static final int UNAUTHORIZED = 1002;
     public static final int FORBIDDEN = 1004;
     public static final int NOT_FOUND = 1005;
     public static final int CONFLICT = 1006;
     public static final int BIZ_ERROR = 1007;
     public static final int RATE_LIMITED = 1008;
     public static final int SERVER_ERROR = 1999;
 
     // User: 2000-2999
     // Order: 3000-3999
     // Feedback: 4000-4999
 
     private ApiCode() {}
 }

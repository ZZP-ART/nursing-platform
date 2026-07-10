package com.nursing.user.constant;

public final class UserErrorCode {

    public static final int TOKEN_BLACKLISTED = 1003;

    public static final int SMS_SEND_TOO_FREQUENT = 2001;
    public static final int SMS_DAILY_LIMIT_REACHED = 2002;
    public static final int PHONE_ALREADY_REGISTERED = 2003;
    public static final int PHONE_NOT_REGISTERED = 2004;
    public static final int SMS_SEND_FAILED = 2005;
    public static final int SMS_CODE_INVALID = 2006;
    public static final int SMS_CODE_EXPIRED = 2007;
    public static final int REGISTER_PHONE_CONFLICT = 2008;
    public static final int PASSWORD_WEAK = 2009;
    public static final int PASSWORD_INVALID = 2010;
    public static final int ACCOUNT_DISABLED = 2011;
    public static final int PASSWORD_SAME_AS_OLD = 2012;
    public static final int ID_CARD_INVALID = 2013;
    public static final int FILE_TOO_LARGE = 2014;
    public static final int FILE_UPLOAD_FAILED = 2015;
    public static final int PROFILE_VERSION_CONFLICT = 2016;
    public static final int FILE_IDEMPOTENCY_CONFLICT = 2017;
    public static final int FILE_UPLOAD_PROCESSING = 2018;

    private UserErrorCode() {
    }
}

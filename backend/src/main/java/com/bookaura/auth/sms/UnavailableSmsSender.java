package com.bookaura.auth.sms;

import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
public class UnavailableSmsSender implements SmsSender {

    @Override
    public void sendOtp(String phone, String code) {
        throw new BusinessException(ErrorCode.SMS_DELIVERY_UNAVAILABLE,
                "SMS delivery is not configured in this environment");
    }
}

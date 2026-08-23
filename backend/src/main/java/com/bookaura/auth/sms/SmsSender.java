package com.bookaura.auth.sms;

public interface SmsSender {
    void sendOtp(String phone, String code);
}

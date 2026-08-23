package com.bookaura.auth.sms;

import com.bookaura.common.util.PhoneNormalizer;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** ADMIN-only local demo aid. Never registered in test/prod and never logs raw codes. */
@Hidden
@RestController
@Profile("local")
@RequestMapping("/api/admin/dev/sms-outbox")
@PreAuthorize("hasRole('ADMIN')")
public class FakeSmsOutboxController {

    private final FakeSmsSender sender;

    public FakeSmsOutboxController(FakeSmsSender sender) {
        this.sender = sender;
    }

    @GetMapping("/latest")
    public FakeSmsResponse latest(@RequestParam String phone) {
        FakeSmsSender.SentSms message = sender.lastTo(PhoneNormalizer.normalize(phone));
        return new FakeSmsResponse(message.phone(), message.code(), message.sentAt());
    }

    public record FakeSmsResponse(String phone, String code, java.time.Instant sentAt) {
    }
}

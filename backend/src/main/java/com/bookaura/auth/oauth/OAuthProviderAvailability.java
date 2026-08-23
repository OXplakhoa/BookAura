package com.bookaura.auth.oauth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

@Component
public class OAuthProviderAvailability {

    private final ObjectProvider<ClientRegistrationRepository> registrations;

    public OAuthProviderAvailability(ObjectProvider<ClientRegistrationRepository> registrations) {
        this.registrations = registrations;
    }

    public boolean isGoogleConfigured() {
        ClientRegistrationRepository repository = registrations.getIfAvailable();
        return repository != null && repository.findByRegistrationId("google") != null;
    }
}

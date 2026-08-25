package com.bookaura.auth.sms;

/** Non-secret SMS provider settings; the API key is supplied only through environment binding. */
public class SmsProperties {

    private String provider = "unavailable";
    private Brevo brevo = new Brevo();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Brevo getBrevo() {
        return brevo;
    }

    public void setBrevo(Brevo brevo) {
        this.brevo = brevo;
    }

    public boolean isBrevoConfigured() {
        return "brevo".equalsIgnoreCase(provider == null ? "" : provider.trim())
                && brevo != null
                && brevo.getApiKey() != null
                && !brevo.getApiKey().isBlank();
    }

    public static class Brevo {
        private String baseUrl = "https://api.brevo.com/v3";
        private String apiKey = "";
        private String sender = "BookAura";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }
    }
}

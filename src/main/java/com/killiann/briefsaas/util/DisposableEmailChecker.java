package com.killiann.briefsaas.util;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

@Component
public class DisposableEmailChecker {

    private final Set<String> disposableDomains = new HashSet<>();

    @PostConstruct
    public void loadDisposableDomains() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("disposable_domains.txt")) {
            assert is != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    disposableDomains.add(line.trim().toLowerCase());
                }
            }
        }
    }

    public boolean isDisposable(String email) {
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        return disposableDomains.contains(domain);
    }
}
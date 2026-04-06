package com.bolas.ecommerce.service;

import org.springframework.stereotype.Service;

@Service
public class InputSanitizerService {

    public String sanitizeText(String value) {
        if (value == null) {
            return null;
        }
        String s = value
                .replaceAll("(?is)<script.*?>.*?</script>", "")
                .replaceAll("(?is)<style.*?>.*?</style>", "")
                .replaceAll("(?is)<[^>]+>", "")
                .replaceAll("[\\u0000-\\u001F\\u007F]", "")
                .trim();
        return s.isEmpty() ? null : s;
    }
}


package com.listifyai.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EtsyUrlParserTest {

    private final EtsyUrlParser parser = new EtsyUrlParser();

    @Test
    void parseValidEtsyUrls() {
        var result = parser.parse("https://www.etsy.com/listing/1234567/some-product");
        assertEquals("1234567", result.listingId());
        assertEquals("https://www.etsy.com/listing/1234567", result.canonicalUrl());

        var result2 = parser.parse("https://etsy.com/listing/9876543");
        assertEquals("9876543", result2.listingId());
        assertEquals("https://www.etsy.com/listing/9876543", result2.canonicalUrl());
    }

    @Test
    void parseInvalidEtsyUrlsThrows400() {
        assertThrows(ResponseStatusException.class, () -> parser.parse("https://amazon.com/listing/123"));
        assertThrows(ResponseStatusException.class, () -> parser.parse("not-a-url"));
        assertThrows(ResponseStatusException.class, () -> parser.parse("https://etsy.com/shop/MyShop"));
        assertThrows(ResponseStatusException.class, () -> parser.parse("https://etsy.com/listing/abc"));
    }
}

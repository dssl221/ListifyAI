package com.listifyai.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EtsyUrlParser {

    private static final Pattern LISTING_PATTERN = Pattern.compile("/listing/(\\d+)(?:/|$)");

    public ParsedListingUrl parse(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank() || rawUrl.length() > 1000) {
            throw invalidUrl();
        }

        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException ex) {
            throw invalidUrl();
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme) || !(host.equals("etsy.com") || host.equals("www.etsy.com"))) {
            throw invalidUrl();
        }

        Matcher matcher = LISTING_PATTERN.matcher(uri.getPath() == null ? "" : uri.getPath());
        if (!matcher.find()) {
            throw invalidUrl();
        }

        String listingId = matcher.group(1);
        return new ParsedListingUrl(listingId, "https://www.etsy.com/listing/" + listingId);
    }

    private ResponseStatusException invalidUrl() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Paste a valid public Etsy listing URL, like https://www.etsy.com/listing/123456789/product-name."
        );
    }

    public record ParsedListingUrl(String listingId, String canonicalUrl) {}
}

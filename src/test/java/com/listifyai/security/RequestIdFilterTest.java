package com.listifyai.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RequestIdFilterTest {

    @Test
    void replacesUnsafeClientProvidedRequestId() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/listings");
        request.addHeader("X-Request-Id", "trusted\r\nforged-log-entry");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader("X-Request-Id");
        assertFalse(requestId.contains("\r"));
        assertFalse(requestId.contains("\n"));
        assertEquals(36, requestId.length());
    }
}

package com.listifyai.security;
import jakarta.servlet.http.HttpServletRequest; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Component; import java.util.*;
@Component
public class TrustedProxyClientIpResolver {
    private final Set<String> trusted;
    public TrustedProxyClientIpResolver(@Value("${rate.limit.trusted-proxies:}") String values){trusted=Arrays.stream(values.split(",")).map(String::trim).filter(s->!s.isBlank()).collect(java.util.stream.Collectors.toUnmodifiableSet());}
    public String resolve(HttpServletRequest request){
        String remote=request.getRemoteAddr(); if(!trusted.contains(remote)) return remote;
        String xff=request.getHeader("X-Forwarded-For"); if(xff!=null&&!xff.isBlank()) return xff.split(",",2)[0].trim();
        String real=request.getHeader("X-Real-IP"); return real==null||real.isBlank()?remote:real.trim();
    }
}

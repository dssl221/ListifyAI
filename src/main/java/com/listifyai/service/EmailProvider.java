package com.listifyai.service;
public interface EmailProvider { String send(String recipient, String subject, String html); }

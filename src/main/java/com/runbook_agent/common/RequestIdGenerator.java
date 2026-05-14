package com.runbook_agent.common;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RequestIdGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicLong counter = new AtomicLong(0);

    public String generate() {
        String date = LocalDate.now().format(DATE_FORMAT);
        long seq = counter.incrementAndGet();
        return String.format("REQ-%s-%06d", date, seq);
    }
}

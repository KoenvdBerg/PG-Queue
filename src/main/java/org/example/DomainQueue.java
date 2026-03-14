package org.example;

import java.time.Instant;

public record DomainQueue(long id, int status, Instant updated_at, String payload) {};

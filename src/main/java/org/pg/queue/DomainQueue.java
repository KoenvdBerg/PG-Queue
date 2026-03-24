package org.pg.queue;

import java.time.Instant;

public record DomainQueue(long id, int status, Instant updated_at, String updated_by, String payload) {};

package com.pdcm.forecast;

import java.time.Instant;

public record HistoryPoint(long userId, Instant observedAt, int intensity) {
}

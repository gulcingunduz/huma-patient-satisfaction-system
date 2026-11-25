package com.hbys.survey.application.service.impl;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AISummaryServiceImplTest {

    @Test
    void shouldConvertBusinessDayBoundariesToUtc() {
        LocalDate date = LocalDate.of(2026, 1, 1);

        assertThat(AISummaryServiceImpl.startOfBusinessDay(date))
                .isEqualTo(Instant.parse("2025-12-31T21:00:00Z"));
        assertThat(AISummaryServiceImpl.endOfBusinessDay(date))
                .isEqualTo(Instant.parse("2026-01-01T20:59:59.999999999Z"));
    }
}
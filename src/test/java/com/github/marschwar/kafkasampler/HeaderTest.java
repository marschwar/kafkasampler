package com.github.marschwar.kafkasampler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeaderTest {

    @Test
    void fromString() {
        assertEquals(new Header("foo", "bar"), Header.fromString("foo|#|bar"));
    }
}

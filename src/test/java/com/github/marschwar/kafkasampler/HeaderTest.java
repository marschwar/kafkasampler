package com.github.marschwar.kafkasampler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HeaderTest {

    @Test
    void fromString() {
        assertThat(Header.fromString("foo|#|bar")).isEqualTo(new Header("foo", "bar"));
    }
}

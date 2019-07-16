package com.github.marschwar.kafkasampler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderTest {

    @Test
    void fromString() {
        assertThat(Header.fromString("foo|#|bar")).isEqualTo(new Header("foo", "bar"));
    }

    @Test
    void defaultToNullIfEmpty() {
        assertThat(Header.fromString("|#|")).isNull();
    }
}

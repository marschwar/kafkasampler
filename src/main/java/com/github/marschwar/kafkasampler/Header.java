package com.github.marschwar.kafkasampler;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

class Header {
    private static final String SEPARATOR = "|#|";

    String key;
    String value;

    public Header() {
    }

    public Header(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Header header = (Header) o;
        return Objects.equals(key, header.key) &&
            Objects.equals(value, header.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return key + SEPARATOR + value;
    }

    static Header fromString(String s) {
        if (s == null) return null;

        int idx = s.indexOf(SEPARATOR);
        final Header header = new Header();
        if (idx >= 0) {
            header.key = s.substring(0, idx);
            header.value = s.substring(idx + SEPARATOR.length());
        }

        if (StringUtils.isAllBlank(header.key, header.value)) {
            return null;
        }

        return header;
    }
}

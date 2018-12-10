package org.jdbi.v3.lib.internal.expiringmap;

import org.jdbi.v3.lib.internal.expiringmap.net.jodah.expiringmap.ExpiringMap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpiringMapSanityTest {
    @Test
    public void expiringMapCorrectlyLoaded() {
        assertThat(ExpiringMap.class.getPackage().getName()).isEqualTo("org.jdbi.v3.lib.internal.expiringmap.net.jodah.expiringmap");
    }
}

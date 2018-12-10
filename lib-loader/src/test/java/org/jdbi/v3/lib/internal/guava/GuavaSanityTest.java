package org.jdbi.v3.lib.internal.guava;

import org.jdbi.v3.lib.internal.guava.com.google.common.base.Objects;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GuavaSanityTest {
    @Test
    public void guavaCorrectlyLoaded() {
        assertThat(Objects.class.getPackage().getName()).isEqualTo("org.jdbi.v3.lib.internal.guava.com.google.common.base");
    }
}

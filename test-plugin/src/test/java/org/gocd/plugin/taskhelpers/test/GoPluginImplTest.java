package org.gocd.plugin.taskhelpers.test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GoPluginImplTest {
    @Test
    public void shouldCleanupScript() {
        assertThatThrownBy(() -> new GoPluginImpl().handle(null))
          .isInstanceOf(UnsupportedOperationException.class);
    }
}
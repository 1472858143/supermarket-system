package com.supermarket.inventory.common.controller;

import com.supermarket.inventory.common.response.ApiResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    @Test
    void health_returnsUpStatusWithoutBusinessData() {
        HealthController controller = new HealthController();

        ApiResponse<Map<String, String>> response = controller.health();

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).containsEntry("status", "UP");
        assertThat(response.getData()).containsEntry("service", "supermarket-inventory");
    }
}

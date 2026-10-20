package com.dev.isac_plus.dto;

import tools.jackson.databind.JsonNode;

// "data" muda de formato conforme o evento, por isso fica como JSON livre.
public record EvolutionWebhookEvento(String event, String instance, JsonNode data) {
}

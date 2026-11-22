package net.ankan.budget.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class AiChatRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
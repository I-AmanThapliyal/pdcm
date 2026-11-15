package com.pdcm.moderation;

import jakarta.validation.constraints.Size;

public record ModerationActionRequest(
		@Size(max = 500) String note
) {
}

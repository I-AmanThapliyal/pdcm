package com.pdcm.suggestion;

import jakarta.validation.constraints.Size;

public record SuggestionActionRequest(
		@Size(max = 500) String note
) {
}

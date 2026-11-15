package com.pdcm.externaltraffic;

public class ProviderUnavailableException extends RuntimeException {

	private final String errorCode;

	public ProviderUnavailableException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}
}

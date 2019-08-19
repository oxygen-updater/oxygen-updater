package com.arjanvlek.oxygenupdater.internal.server;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerPostResult {

	private boolean success;

	@JsonProperty("error_message")
	private String errorMessage;
}

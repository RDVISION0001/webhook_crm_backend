package com.crm.rdvision.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuccessResponse {

	public final String status="200";
	public String message;
	
	public SuccessResponse(String message) {
		super();
		this.message = message;
	}
	
	public SuccessResponse() {
	}
	
}

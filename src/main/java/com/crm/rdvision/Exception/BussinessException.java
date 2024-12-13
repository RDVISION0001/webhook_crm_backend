package com.crm.rdvision.Exception;

import org.springframework.http.HttpStatus;

public class BussinessException extends Throwable {

	private static final long serialVersionUID = 1L;
	
	private HttpStatus errorcode;
	
	private String description;

	private String parameter;
	
	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public HttpStatus getErrorcode() {
		return errorcode;
	}

	public void setErrorcode(HttpStatus errorcode) {
		this.errorcode = errorcode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BussinessException(HttpStatus errorcode, String description) {
		super();
		this.errorcode=errorcode;
		this.description=description;
	}

	/**
	 * 
	 * @param errorcode
	 * @param description
	 * @param parameter
	 */
	public BussinessException(HttpStatus errorcode, String description, String parameter) {
		super();
		this.parameter=parameter;
		this.errorcode=errorcode;
		this.description=description;
	}

}

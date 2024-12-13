package com.avanse.core.exception;

import org.springframework.http.HttpStatus;

public class TechnicalException extends Throwable{

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

    public TechnicalException(String description,Throwable e,HttpStatus errorcode) {
        super(description,e);
        this.description=description;
        this.errorcode=errorcode;
    }


    public TechnicalException(String description,HttpStatus errorcode) {
        super(description);
        this.description=description;
        this.errorcode=errorcode;
    }
    public TechnicalException(String description,HttpStatus errorcode,String parameter) {
        super(description);
        this.parameter=parameter;
        this.description=description;
        this.errorcode=errorcode;
    }




}

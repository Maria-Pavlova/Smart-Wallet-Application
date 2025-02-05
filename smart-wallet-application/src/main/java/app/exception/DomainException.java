package app.exception;

import org.springframework.http.HttpStatus;

public class DomainException extends RuntimeException{


    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, HttpStatus cause) {
        super();
    }
}

package com.example.urlshortener.exception;

public class CodeGenerationException extends RuntimeException {

    public CodeGenerationException(int attempts) {
        super("Failed to generate a unique short code after " + attempts + " attempts");
    }
}

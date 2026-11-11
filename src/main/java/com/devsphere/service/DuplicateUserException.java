package com.devsphere.service;

public class DuplicateUserException extends Exception {

    public DuplicateUserException() {
        super("That username or email is already registered.");
    }
}

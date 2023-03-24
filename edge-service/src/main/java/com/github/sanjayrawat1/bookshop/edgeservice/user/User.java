package com.github.sanjayrawat1.bookshop.edgeservice.user;

import java.util.List;

/**
 * @author Sanjay Singh Rawat
 */
public record User(String username, String firstName, String lastName, List<String> roles) {}

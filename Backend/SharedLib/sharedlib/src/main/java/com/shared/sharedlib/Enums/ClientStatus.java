package com.shared.sharedlib.Enums;

import lombok.Getter;

@Getter
public enum ClientStatus {

    Active("This User is active"),
    Suspended("This Client is Suspended");

    private final String description;

    // Constructor for enum
    ClientStatus(String description) {
        this.description = description;
    }
}

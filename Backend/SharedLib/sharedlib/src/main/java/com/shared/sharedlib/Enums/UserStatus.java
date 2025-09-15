package com.shared.sharedlib.Enums;

import lombok.Getter;

@Getter
public enum UserStatus {

    Active("This User is active"),
    Inactive("This User is inactive"),
    Suspended("This User has been Suspended");

    private final String description;

    // Constructor for enum
    UserStatus(String description) {
        this.description = description;
    }
}

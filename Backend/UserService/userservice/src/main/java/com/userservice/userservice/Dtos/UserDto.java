package com.userservice.userservice.Dtos;

import com.shared.sharedlib.Enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {

    private BigDecimal id;

    private String userName;

    private String userEmail;

    private String userPhone;

    private String userPassword;

    private String userRole;

    private UserStatus status;

}

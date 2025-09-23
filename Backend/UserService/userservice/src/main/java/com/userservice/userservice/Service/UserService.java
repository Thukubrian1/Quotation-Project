package com.userservice.userservice.Service;

import com.shared.sharedlib.Dtos.GenericResponse;
import com.userservice.userservice.Dtos.UserDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface UserService {

    GenericResponse<UserDto> getUserByUsername(@RequestParam String userName);

    GenericResponse<UserDto> addUser(@RequestBody UserDto userDto, String userEmail);

    GenericResponse<UserDto> loginUser(@RequestParam String userEmail, String userPassword);

}

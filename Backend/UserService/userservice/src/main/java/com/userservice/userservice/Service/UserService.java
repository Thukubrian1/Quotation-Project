package com.userservice.userservice.Service;

import com.shared.sharedlib.Dtos.GenericResponse;
import com.shared.sharedlib.Entity.User;
import com.userservice.userservice.Dtos.UserDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface UserService {

    GenericResponse<User> getUserByUsername(@RequestParam String userName);

    GenericResponse<UserDto> addUser(@RequestBody UserDto userDto, String userEmail);

}

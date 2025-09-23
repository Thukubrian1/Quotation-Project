package com.userservice.userservice.ServiceImpl;

import com.shared.sharedlib.Dtos.GenericResponse;
import com.shared.sharedlib.Entity.User;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import com.shared.sharedlib.Enums.UserStatus;
import com.userservice.userservice.Dtos.UserDto;
import com.userservice.userservice.Repository.UserRepository;
import com.userservice.userservice.Service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
    @Override
    public GenericResponse<UserDto> getUserByUsername(String userName) {
        try {
            Optional<User> existingUser = userRepository.findByUserName(userName);

            if (existingUser.isPresent()) {
                User user = existingUser.get();

                UserDto responseDto = UserDto.builder()
                        .userEmail(user.getUserEmail())
                        .userName(user.getUserName())
                        .userPhone(user.getUserPhone())
                        .userRole(user.getUserRole())
                        .status(user.getStatus())
                        .build();

                return GenericResponse.<UserDto>builder()
                        .status(ResponseStatusEnum.SUCCESS)
                        .message("User retrieved successfully")
                        .data(responseDto)
                        .build();
            } else {
                return GenericResponse.<UserDto>builder()
                        .status(ResponseStatusEnum.NOT_FOUND)
                        .message("User not found with username: " + userName)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error retrieving user with username: {}", userName, e);
            return GenericResponse.<UserDto>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("Failed to retrieve user")
                    .debugMessage(e.getMessage())
                    .build();
        }
    }


    @Override
    public GenericResponse<UserDto> addUser(UserDto userDto, String userEmail) {

        try{

            User existingUser = userRepository.findByUserEmail(userEmail);

            if (existingUser != null) {

                return GenericResponse.<UserDto>builder()
                        .status(ResponseStatusEnum.CONFLICT)
                        .message("User with already exists")
                        .data(null)
                        .build();
            }

            else{

                String hashedPassword = passwordEncoder.encode(userDto.getUserPassword());

                User newUser = User.builder()
                        .userEmail(userDto.getUserEmail())
                        .userName(userDto.getUserName())
                        .userPhone(userDto.getUserPhone())
                        .userRole(userDto.getUserRole())
                        .userPassword(hashedPassword)
                        .status(userDto.getStatus())
                        .build();

                userRepository.save(newUser);

                return GenericResponse.<UserDto>builder()
                        .status(ResponseStatusEnum.SUCCESS)
                        .message("User Created Successfully")
                        .data(userDto)
                        .build();
            }
        }

        catch (Exception e){

            log.error("Error creating user ", e);
            return GenericResponse.<UserDto>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("Failed to create user")
                    .debugMessage(e.getMessage())
                    .build();

        }
    }

// In your UserServiceImpl.java, modify the loginUser method to return user info:

    @Override
    public GenericResponse<UserDto> loginUser(String userEmail, String userPassword) {
        try{

            User existingUser = userRepository.findByUserEmail(userEmail);

            if (existingUser != null) {

                if (existingUser.getStatus() == UserStatus.Inactive) {
                    return GenericResponse.<UserDto>builder()
                            .status(ResponseStatusEnum.UNAUTHORIZED)
                            .message("User account is inactive")
                            .data(null)
                            .build();
                }

                if (existingUser.getStatus() == UserStatus.Suspended) {
                    return GenericResponse.<UserDto>builder()
                            .status(ResponseStatusEnum.UNAUTHORIZED)
                            .message("User account is suspended")
                            .data(null)
                            .build();
                }

                if (passwordEncoder.matches(userPassword, existingUser.getUserPassword())) {

                    UserDto responseDto = UserDto.builder()
                            .userEmail(existingUser.getUserEmail())
                            .userName(existingUser.getUserName())  // Return actual username
                            .userPhone(existingUser.getUserPhone())
                            .userRole(existingUser.getUserRole())
                            .status(existingUser.getStatus())
                            .build();

                    return GenericResponse.<UserDto>builder()
                            .status(ResponseStatusEnum.SUCCESS)
                            .message("Login Successful")
                            .data(responseDto)
                            .build();
                } else {
                    return GenericResponse.<UserDto>builder()
                            .status(ResponseStatusEnum.UNAUTHORIZED)
                            .message("Invalid email or password")
                            .data(null)
                            .build();
                }
            } else {
                return GenericResponse.<UserDto>builder()
                        .status(ResponseStatusEnum.UNAUTHORIZED)
                        .message("Invalid email or password")
                        .data(null)
                        .build();
            }
        }
        catch (Exception e){
            log.error("Error during login for email: {}", userEmail, e);

            return GenericResponse.<UserDto>builder()
                    .status(ResponseStatusEnum.ERROR)
                    .message("An error occurred during login")
                    .debugMessage(e.getMessage())
                    .build();
        }
    }

    // use when i want to decode password
//    passwordEncoder.matches(rawPassword, storedHash);

}

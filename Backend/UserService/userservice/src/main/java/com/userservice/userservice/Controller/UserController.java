package com.userservice.userservice.Controller;

import com.shared.sharedlib.Dtos.GenericResponse;
import com.shared.sharedlib.Entity.User;
import com.userservice.userservice.Dtos.UserDto;
import com.userservice.userservice.ServiceImpl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/users")
public class UserController {

    private final UserServiceImpl userService;

    @GetMapping("/getuser")
    public ResponseEntity<GenericResponse<User>> getUser(@RequestParam(name = "userName", required = true) String userName) {
        try {
            log.debug("Received request to get user with userName: '{}'", userName);

            String cleanUserName = userName.trim();

            if (cleanUserName.isEmpty()) {
                GenericResponse<User> errorResponse = GenericResponse.<User>builder()
                        .status(com.shared.sharedlib.Enums.ResponseStatusEnum.ERROR)
                        .message("Username parameter cannot be empty")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            GenericResponse<User> response = userService.getUserByUsername(cleanUserName);

            return switch (response.getStatus()) {
                case SUCCESS -> ResponseEntity.ok(response);
                case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            };

        }
        catch (Exception e) {
            log.error("Unexpected error in getUser: {}", e.getMessage(), e);
            GenericResponse<User> errorResponse = GenericResponse.<User>builder()
                    .status(com.shared.sharedlib.Enums.ResponseStatusEnum.ERROR)
                    .message("An unexpected error occurred")
                    .debugMessage(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/adduser")
    public ResponseEntity<GenericResponse<UserDto>> createUser(@RequestBody UserDto userDto) {
        try {
            String userEmail = userDto.getUserEmail();
            GenericResponse<UserDto> response = userService.addUser(userDto, userEmail);

            return switch (response.getStatus()) {
                case SUCCESS -> ResponseEntity.status(HttpStatus.CREATED).body(response);
                case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            };
        }
        catch (Exception e) {
            log.error("Unexpected error in createUser: {}", e.getMessage(), e);
            GenericResponse<UserDto> errorResponse = GenericResponse.<UserDto>builder()
                    .status(com.shared.sharedlib.Enums.ResponseStatusEnum.ERROR)
                    .message("An unexpected error occurred while creating user")
                    .debugMessage(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
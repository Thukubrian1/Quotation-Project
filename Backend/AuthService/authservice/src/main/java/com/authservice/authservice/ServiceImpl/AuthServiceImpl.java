package com.authservice.authservice.ServiceImpl;

import com.authservice.authservice.Repository.AuthRepository;
import com.authservice.authservice.Service.AuthService;
import com.authservice.authservice.Utility.JWTUtil;
import com.shared.sharedlib.Entity.User;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import com.shared.sharedlib.Enums.UserStatus;
import com.shared.sharedlib.Exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final JWTUtil jwtUtil;

    @Override
    public String login(String username, String password){
        try {

            User existingUser = authRepository.findByUserName(username);

            if(existingUser == null){
                log.warn("Invalid login attempt for username: {}", username);
                throw new BusinessException(ResponseStatusEnum.UNAUTHORIZED, "User not found with name " + username);
            }

            if(existingUser.getStatus() == UserStatus.Inactive){
                log.warn("Login attempt for inactive user: {}", username);
                throw new BusinessException(ResponseStatusEnum.UNAUTHORIZED, "User account is inactive");
            }

            if(existingUser.getStatus() == UserStatus.Suspended){
                log.warn("Login attempt for suspended user: {}", username);
                throw new BusinessException(ResponseStatusEnum.UNAUTHORIZED, "User account is suspended");
            }

            if(existingUser.getUserPassword() == null || !existingUser.getUserPassword().equals(password)){
                log.warn("Invalid password attempt for username: {}", username);
                throw new BusinessException(ResponseStatusEnum.UNAUTHORIZED, "Invalid username or password");
            }

            String token = jwtUtil.generateToken(username);
            log.info("Successful login for username: {}", username);
            return token;

        }
        catch (BusinessException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Unexpected error during login for username: {}", username, e);
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "Could not process login request",
                    e.getMessage()
            );
        }
    }
}

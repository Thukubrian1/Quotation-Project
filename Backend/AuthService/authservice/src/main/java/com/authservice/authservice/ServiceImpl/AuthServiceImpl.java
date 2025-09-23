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

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final JWTUtil jwtUtil;

    @Override
    public String login(String userName, String userEmail) {
        try {
            log.debug("Attempting login for username: {} and email: {}", userName, userEmail);

            User existingUser = authRepository.findByUserName(userName);

            if (existingUser == null) {
                log.warn("Invalid login attempt - user not found for username: {}", userName);
                throw new BusinessException(ResponseStatusEnum.UNAUTHORIZED, "Invalid username or password");
            }

            if (existingUser.getStatus() == UserStatus.Inactive) {
                log.warn("Login attempt for inactive user: {}", userName);
                throw new BusinessException(ResponseStatusEnum.UNAUTHORIZED, "User account is inactive");
            }

            if (existingUser.getStatus() == UserStatus.Suspended) {
                log.warn("Login attempt for suspended user: {}", userName);
                throw new BusinessException(ResponseStatusEnum.UNAUTHORIZED, "User account is suspended");
            }

            if (existingUser.getUserEmail() == null || !existingUser.getUserEmail().equals(userEmail)) {
                log.warn("Email mismatch for user: {}. Expected: {}, Got: {}",
                        userName, existingUser.getUserEmail(), userEmail);
                throw new BusinessException(ResponseStatusEnum.UNAUTHORIZED, "Invalid username or password");
            }


            if(existingUser.getUserRole() == null || !existingUser.getUserRole().equals("Admin")){

                log.warn("Email mismatch for user: {}. Expected: {}, Got: {}",
                        userName, existingUser.getUserEmail(), userEmail);
                throw new BusinessException(ResponseStatusEnum.UNAUTHORIZED, "User must have admin Role");
            }
            String token = jwtUtil.generateToken(userName);
            log.info("Successful login for username: {}", userName);
            return token;

        }
        catch (BusinessException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Unexpected error during login for username: {}", userName, e);
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "Could not process login request",
                    e.getMessage()
            );
        }
    }
}
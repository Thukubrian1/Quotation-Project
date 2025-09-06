package com.authservice.authservice.ServiceImpl;

import com.authservice.authservice.Entity.InMemoryUserStore;
import com.authservice.authservice.Service.AuthService;
import com.authservice.authservice.Utility.JWTUtil;
import com.shared.sharedlib.Enums.ResponseStatusEnum;
import com.shared.sharedlib.Exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final InMemoryUserStore userStore;
    private final JWTUtil jwtUtil;

    @Override
    public String login(String username, String password) {
        try {
            if (!userStore.validate(username, password)) {
                log.warn("Invalid login attempt for username: {}", username);
                throw new BusinessException(
                        ResponseStatusEnum.UNAUTHORIZED,
                        "Invalid username or password",
                        "Authentication failed for user: " + username
                );
            }

            String token = jwtUtil.generateToken(username);
            log.info("Successful login for username: {}", username);
            return token;

        } catch (BusinessException e) {
            // Re-throw business exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for username: {}", username, e);
            throw new BusinessException(
                    ResponseStatusEnum.ERROR,
                    "Could not process login request",
                    e.getMessage()
            );
        }
    }
}

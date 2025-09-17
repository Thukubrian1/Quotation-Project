package com.userservice.userservice.Repository;

import com.shared.sharedlib.Entity.User;
import com.userservice.userservice.Dtos.UserDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String userName);

    User findByUserEmail(String userEmail);

}
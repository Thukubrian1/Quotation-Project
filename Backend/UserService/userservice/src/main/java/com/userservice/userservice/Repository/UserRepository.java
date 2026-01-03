package com.userservice.userservice.Repository;

import com.shared.sharedlib.Entity.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String userName);

    User findByUserEmail(String userEmail);

    User findByUserEmailAndUserPassword(String userEmail, String userPassword);
}
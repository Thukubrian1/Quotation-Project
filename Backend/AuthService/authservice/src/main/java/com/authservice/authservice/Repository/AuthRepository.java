package com.authservice.authservice.Repository;

import com.shared.sharedlib.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<User, Long> {

    User findByUserName(String userName);

}

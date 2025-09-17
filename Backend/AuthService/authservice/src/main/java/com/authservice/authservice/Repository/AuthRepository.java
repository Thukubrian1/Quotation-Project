package com.authservice.authservice.Repository;

import com.shared.sharedlib.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;

public interface AuthRepository extends JpaRepository<User, Long> {


    User findByUserName(String userName);


}

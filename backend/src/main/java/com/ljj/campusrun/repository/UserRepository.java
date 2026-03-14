package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.enums.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByStudentNo(String studentNo);

    boolean existsByRole(UserRole role);

    List<User> findByRole(UserRole role);
}

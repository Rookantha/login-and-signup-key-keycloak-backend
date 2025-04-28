package com.contentnexus.iam.service.repository;

import com.contentnexus.iam.service.model.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findTop5ByUsernameOrderByTimestampDesc(String username);
}

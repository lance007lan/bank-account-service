package com.bank.accountservice.repository;

import com.bank.accountservice.entity.OffensiveWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OffensiveWordRepository extends JpaRepository<OffensiveWord, Long> {

    @Query("SELECT COUNT(o) > 0 FROM OffensiveWord o WHERE LOWER(:text) LIKE CONCAT('%', LOWER(o.word), '%')")
    boolean containsOffensiveWord(@Param("text") String text);
}

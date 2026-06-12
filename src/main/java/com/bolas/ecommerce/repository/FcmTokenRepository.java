package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.FcmToken;
import com.bolas.ecommerce.model.NotificationDestinataire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findByUserIdAndUserType(Long userId, NotificationDestinataire userType);

    Optional<FcmToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM FcmToken t WHERE t.token = :token")
    void deleteByToken(@Param("token") String token);

    @Query("SELECT t.token FROM FcmToken t WHERE t.userId = :userId AND t.userType = :userType")
    List<String> findTokenStringsByUserIdAndUserType(@Param("userId") Long userId,
                                                      @Param("userType") NotificationDestinataire userType);
}

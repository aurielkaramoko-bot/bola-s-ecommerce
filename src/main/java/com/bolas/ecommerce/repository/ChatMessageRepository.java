package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.ChatMessage;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Messages d'une conversation vendeur↔client */
    List<ChatMessage> findByVendorAndCustomerIdentifierOrderBySentAtAsc(
            VendorUser vendor, String customerIdentifier);

    /** Derniers messages par conversation (inbox vendeur) */
    @Query("SELECT DISTINCT m.customerIdentifier FROM ChatMessage m WHERE m.vendor = :vendor")
    List<String> findDistinctCustomersByVendor(VendorUser vendor);

    /** Nombre de messages non lus par le vendeur */
    long countByVendorAndReadByVendorFalseAndSenderType(VendorUser vendor, String senderType);

    /** Dernier message d'une conversation */
    ChatMessage findFirstByVendorAndCustomerIdentifierOrderBySentAtDesc(
            VendorUser vendor, String customerIdentifier);
}

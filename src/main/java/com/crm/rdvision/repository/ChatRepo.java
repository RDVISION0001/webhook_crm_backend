package com.crm.rdvision.repository;

import com.crm.rdvision.entity.Messages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepo extends JpaRepository<Messages,Long> {

    List<Messages> findBySentByUserIdOrSentToUserId(Long userId1, Long userId2);
}

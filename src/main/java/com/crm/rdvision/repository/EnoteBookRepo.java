package com.crm.rdvision.repository;

import com.crm.rdvision.entity.EnoteBook;
import com.crm.rdvision.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EnoteBookRepo extends JpaRepository<EnoteBook,Long> {

    List<EnoteBook> findByUser(User user);
    EnoteBook findByNoteId(long noteId);

    @Modifying
    @Transactional
    @Query("DELETE FROM EnoteBook c WHERE c.user.userId = :userId")
    void deleteAllByUser(int userId);
}

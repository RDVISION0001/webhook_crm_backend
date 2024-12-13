package com.crm.rdvision.repository;

import com.crm.rdvision.entity.EmailContentImages;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailContentImageRepo extends JpaRepository<EmailContentImages,Long> {

    EmailContentImages findByImageId(long id);
}

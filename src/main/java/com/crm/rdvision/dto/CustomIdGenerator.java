package com.crm.rdvision.dto;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.Random;

public class CustomIdGenerator implements IdentifierGenerator {
    private static final Random RANDOM = new Random();

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        int uniqueRandomId = Math.abs(RANDOM.nextInt());
        int additionalRandomValue = Math.abs(RANDOM.nextInt());
        return uniqueRandomId + additionalRandomValue;
    }
}


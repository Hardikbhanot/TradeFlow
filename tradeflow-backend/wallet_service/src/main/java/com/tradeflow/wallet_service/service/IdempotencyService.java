package com.tradeflow.wallet_service.service;

import com.tradeflow.wallet_service.repository.IdempotencyRepository;
import com.tradeflow.wallet_service.model.IdempotencyKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IdempotencyService {

    @Autowired
    private IdempotencyRepository repository;

    public Optional<String> getResponse(String key) {
        return repository.findById(key).map(IdempotencyKey::getResponsePayload);
    }

    public void saveResponse(String key, String response) {
        IdempotencyKey entry = new IdempotencyKey();
        entry.setId(key);
        entry.setResponsePayload(response);
        entry.setCreatedAt(LocalDateTime.now());
        repository.save(entry);
    }
}
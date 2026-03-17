package com.tradeflow.portfolio_service.service;

import com.tradeflow.portfolio_service.entity.WatchlistItem;
import com.tradeflow.portfolio_service.repository.WatchlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistItemRepository repository;

    public List<String> getWatchlistSymbols(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(WatchlistItem::getSymbol)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addToWatchlist(Long userId, String symbol) {
        if (repository.findByUserIdAndSymbol(userId, symbol).isEmpty()) {
            WatchlistItem item = new WatchlistItem();
            item.setUserId(userId);
            item.setSymbol(symbol.toUpperCase());
            repository.save(item);
        }
    }

    @Transactional
    public void removeFromWatchlist(Long userId, String symbol) {
        repository.deleteByUserIdAndSymbol(userId, symbol.toUpperCase());
    }
}

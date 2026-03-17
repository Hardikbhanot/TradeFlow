package com.tradeflow.portfolio_service.controller;

import com.tradeflow.portfolio_service.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public ResponseEntity<List<String>> getWatchlist(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(watchlistService.getWatchlistSymbols(userId));
    }

    @PostMapping("/{symbol}")
    public ResponseEntity<Void> addToWatchlist(@RequestHeader("X-User-Id") Long userId, @PathVariable String symbol) {
        watchlistService.addToWatchlist(userId, symbol);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> removeFromWatchlist(@RequestHeader("X-User-Id") Long userId, @PathVariable String symbol) {
        watchlistService.removeFromWatchlist(userId, symbol);
        return ResponseEntity.ok().build();
    }
}

package com.stockpicker.api.controller;

import com.stockpicker.api.service.WatchlistService;
import com.stockpicker.common.entity.Watchlist;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/watchlists")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public ResponseEntity<List<Watchlist>> getUserWatchlists(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(watchlistService.getUserWatchlists(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Watchlist> getWatchlist(@PathVariable Long id) {
        return ResponseEntity.ok(watchlistService.getWatchlist(id));
    }

    @PostMapping
    public ResponseEntity<Watchlist> createWatchlist(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        @SuppressWarnings("unchecked")
        List<String> tickers = (List<String>) body.get("tickers");
        Watchlist created = watchlistService.createWatchlist(userId, name, tickers);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Watchlist> updateWatchlist(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        @SuppressWarnings("unchecked")
        List<String> tickers = (List<String>) body.get("tickers");
        return ResponseEntity.ok(watchlistService.updateWatchlist(id, name, tickers));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWatchlist(@PathVariable Long id) {
        watchlistService.deleteWatchlist(id);
        return ResponseEntity.noContent().build();
    }
}

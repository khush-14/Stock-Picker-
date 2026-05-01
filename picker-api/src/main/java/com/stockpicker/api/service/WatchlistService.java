package com.stockpicker.api.service;

import com.stockpicker.common.entity.Watchlist;
import com.stockpicker.common.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;

    @Transactional(readOnly = true)
    public List<Watchlist> getUserWatchlists(UUID userId) {
        return watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Watchlist getWatchlist(Long id) {
        return watchlistRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Watchlist not found: " + id));
    }

    @Transactional
    public Watchlist createWatchlist(UUID userId, String name, List<String> tickers) {
        Watchlist watchlist = Watchlist.builder()
            .userId(userId)
            .name(name)
            .tickers(tickers != null ? tickers : List.of())
            .build();
        return watchlistRepository.save(watchlist);
    }

    @Transactional
    public Watchlist updateWatchlist(Long id, String name, List<String> tickers) {
        Watchlist watchlist = getWatchlist(id);
        if (name != null) watchlist.setName(name);
        if (tickers != null) watchlist.setTickers(tickers);
        return watchlistRepository.save(watchlist);
    }

    @Transactional
    public void deleteWatchlist(Long id) {
        if (!watchlistRepository.existsById(id)) {
            throw new NoSuchElementException("Watchlist not found: " + id);
        }
        watchlistRepository.deleteById(id);
    }
}

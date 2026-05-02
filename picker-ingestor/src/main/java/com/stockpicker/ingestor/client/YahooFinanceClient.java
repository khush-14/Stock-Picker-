package com.stockpicker.ingestor.client;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches daily price data from Yahoo Finance's chart API.
 * URL: https://query2.finance.yahoo.com/v8/finance/chart/{TICKER}.NS
 * Falls back to mock data if the API is unavailable.
 */
@Slf4j
@Component
public class YahooFinanceClient {

    private static final String BASE_URL = "https://query2.finance.yahoo.com/v8/finance/chart/";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;
    private final MockFinancialApiClient mockFallback;

    public YahooFinanceClient(MockFinancialApiClient mockFallback) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.mockFallback = mockFallback;
    }

    public PriceData fetchDailyPrice(String ticker) {
        String symbol = ticker.contains(".") ? ticker : ticker + ".NS";
        String url = BASE_URL + symbol + "?range=1d&interval=1d&includePrePost=false";

        try {
            log.info("Fetching price from Yahoo Finance for {}", symbol);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                .timeout(TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Yahoo Finance returned status {} for {}. Falling back to mock.", response.statusCode(), ticker);
                return fromMock(ticker);
            }

            return parseChartResponse(response.body(), ticker);

        } catch (Exception e) {
            log.warn("Yahoo Finance API call failed for {}: {}. Falling back to mock.", ticker, e.getMessage());
            return fromMock(ticker);
        }
    }

    private PriceData parseChartResponse(String json, String ticker) {
        try {
            // Parse regularMarketPrice from the JSON response
            BigDecimal price = extractDecimal(json, "\"regularMarketPrice\":(\\d+\\.?\\d*)");
            BigDecimal prevClose = extractDecimal(json, "\"chartPreviousClose\":(\\d+\\.?\\d*)");
            Long volume = extractLong(json, "\"regularMarketVolume\":(\\d+)");

            if (price == null) {
                log.warn("Could not parse price from Yahoo Finance response for {}", ticker);
                return fromMock(ticker);
            }

            log.info("Yahoo Finance: {} = ₹{}, volume={}", ticker, price, volume);

            return PriceData.builder()
                .price(price)
                .previousClose(prevClose)
                .volume(volume != null ? volume : 0L)
                .recordDate(LocalDate.now())
                .build();

        } catch (Exception e) {
            log.warn("Failed to parse Yahoo Finance response for {}: {}", ticker, e.getMessage());
            return fromMock(ticker);
        }
    }

    private BigDecimal extractDecimal(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        if (m.find()) {
            return new BigDecimal(m.group(1)).setScale(4, RoundingMode.HALF_UP);
        }
        return null;
    }

    private Long extractLong(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        return null;
    }

    private PriceData fromMock(String ticker) {
        var mock = mockFallback.fetchDailyMetrics(ticker);
        return PriceData.builder()
            .price(mock.getPrice())
            .previousClose(mock.getPrice())
            .volume(mock.getVolume())
            .recordDate(LocalDate.now())
            .build();
    }

    @Getter @Setter @Builder
    public static class PriceData {
        private BigDecimal price;
        private BigDecimal previousClose;
        private Long volume;
        private LocalDate recordDate;
    }
}

package com.stockpicker.ingestor.client;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrapes financial data from screener.in for Indian stocks.
 * URL: https://www.screener.in/company/{TICKER}/consolidated/
 *
 * Extracts: P/E, P/B, Debt/Equity, Revenue, Profit, Promoter Holding/Pledging, Auditor.
 * Includes 2-second rate-limiting between requests.
 */
@Slf4j
@Component
public class ScreenerInScraper {

    private static final String BASE_URL = "https://www.screener.in/company/";
    private static final int TIMEOUT_MS = 15000;
    private static final long RATE_LIMIT_MS = 2000;

    private final MockFinancialApiClient mockFallback;

    public ScreenerInScraper(MockFinancialApiClient mockFallback) {
        this.mockFallback = mockFallback;
    }

    public FinancialsData scrapeFinancials(String ticker) {
        String url = BASE_URL + ticker + "/consolidated/";

        try {
            // Rate-limit: 2-second sleep between scraping calls
            Thread.sleep(RATE_LIMIT_MS);

            log.info("Scraping Screener.in for {}: {}", ticker, url);

            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .timeout(TIMEOUT_MS)
                .get();

            FinancialsData.FinancialsDataBuilder builder = FinancialsData.builder();

            // Extract top-level ratios from the company info section
            extractTopRatios(doc, builder);

            // Extract quarterly results from the financial tables
            extractQuarterlyResults(doc, builder);

            // Extract shareholding pattern
            extractShareholding(doc, builder);

            // Extract auditor name
            extractAuditor(doc, builder);

            FinancialsData data = builder.build();
            log.info("Screener.in scraped for {}: PE={}, Debt/Eq={}, Revenue={}",
                     ticker, data.getPeRatio(), data.getDebtToEquity(), data.getRevenue());
            return data;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Scraping interrupted for {}", ticker);
            return fromMock(ticker);
        } catch (Exception e) {
            log.warn("Screener.in scraping failed for {}: {}. Falling back to mock.", ticker, e.getMessage());
            return fromMock(ticker);
        }
    }

    private void extractTopRatios(Document doc, FinancialsData.FinancialsDataBuilder builder) {
        // Screener.in has a top section with key ratios in <li> or <span> elements
        Elements ratioItems = doc.select("#top-ratios li, .company-ratios li, .ratios-table li");

        for (Element item : ratioItems) {
            String text = item.text().toLowerCase();
            String value = extractNumberFromText(item.select(".number, .value, span.nowrap").text());

            if (value == null) continue;

            if (text.contains("stock p/e") || text.contains("p/e")) {
                builder.peRatio(parseBigDecimal(value));
            } else if (text.contains("book value") || text.contains("p/b")) {
                // For P/B we might get book value; handle both
                BigDecimal bv = parseBigDecimal(value);
                if (text.contains("p/b") && bv != null) {
                    builder.pbRatio(bv);
                }
            } else if (text.contains("debt to equity") || text.contains("debt / equity")) {
                builder.debtToEquity(parseBigDecimal(value));
            } else if (text.contains("dividend yield")) {
                builder.dividendYield(parseBigDecimal(value));
            }
        }

        // Also try the main company info section (alternate layout)
        Elements companyInfo = doc.select(".company-info .info span, .company-info li");
        for (Element info : companyInfo) {
            String label = info.select(".name").text().toLowerCase();
            String val = info.select(".number").text().replaceAll("[,%₹Cr]", "").trim();

            if (label.contains("p/e") && !val.isEmpty()) {
                builder.peRatio(parseBigDecimal(val));
            } else if (label.contains("book value") && !val.isEmpty()) {
                builder.pbRatio(parseBigDecimal(val));
            } else if (label.contains("debt") && label.contains("equity") && !val.isEmpty()) {
                builder.debtToEquity(parseBigDecimal(val));
            }
        }
    }

    private void extractQuarterlyResults(Document doc, FinancialsData.FinancialsDataBuilder builder) {
        // Look for the quarterly results table (usually has class "data-table" or id "quarters")
        Element quartersSection = doc.selectFirst("#quarters, section#quarters");
        if (quartersSection == null) {
            quartersSection = doc.selectFirst("section:has(h2:containsOwn(Quarterly))");
        }

        if (quartersSection != null) {
            Elements rows = quartersSection.select("table tbody tr");
            for (Element row : rows) {
                String label = row.select("td:first-child").text().toLowerCase();
                Elements cells = row.select("td");

                if (cells.size() < 2) continue;

                // Get the most recent quarter value (usually the last column before trailing)
                String latestVal = cells.get(cells.size() - 1).text().replaceAll("[,%₹Cr]", "").trim();

                if (label.contains("sales") || label.contains("revenue") || label.contains("net sales")) {
                    builder.revenue(parseBigDecimalCr(latestVal));
                } else if (label.contains("net profit") || label.contains("profit after tax")) {
                    builder.netProfit(parseBigDecimalCr(latestVal));
                } else if (label.contains("operating profit") || label.contains("opm")) {
                    builder.operatingProfit(parseBigDecimalCr(latestVal));
                }
            }
        }
    }

    private void extractShareholding(Document doc, FinancialsData.FinancialsDataBuilder builder) {
        Element shareholdingSection = doc.selectFirst("#shareholding, section#shareholding");
        if (shareholdingSection == null) {
            shareholdingSection = doc.selectFirst("section:has(h2:containsOwn(Shareholding))");
        }

        if (shareholdingSection != null) {
            Elements rows = shareholdingSection.select("table tbody tr");
            for (Element row : rows) {
                String label = row.select("td:first-child").text().toLowerCase();
                Elements cells = row.select("td");

                if (cells.size() < 2) continue;

                String latestVal = cells.get(cells.size() - 1).text().replaceAll("[%]", "").trim();

                if (label.contains("promoter") && !label.contains("pledge")) {
                    builder.promoterHolding(parseBigDecimal(latestVal));
                } else if (label.contains("pledge") || label.contains("pledg")) {
                    builder.promoterPledging(parseBigDecimal(latestVal));
                }
            }
        }
    }

    private void extractAuditor(Document doc, FinancialsData.FinancialsDataBuilder builder) {
        // Look for auditor info in the annual report or company section
        Elements auditorElements = doc.select("p:containsOwn(Auditor), span:containsOwn(Auditor), div:containsOwn(Auditor)");
        for (Element el : auditorElements) {
            String text = el.text();
            // Extract the auditor name after "Auditor" or "Auditors:"
            Pattern p = Pattern.compile("(?:Auditors?\\s*:?\\s*)([A-Z][\\w\\s&,]+)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) {
                builder.auditorName(m.group(1).trim());
                break;
            }
        }
    }

    private String extractNumberFromText(String text) {
        if (text == null || text.isEmpty()) return null;
        String cleaned = text.replaceAll("[,%₹CrLacs]", "").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            if (value == null || value.isEmpty() || value.equals("-")) return null;
            return new BigDecimal(value.replaceAll(",", "")).setScale(4, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimalCr(String value) {
        try {
            if (value == null || value.isEmpty() || value.equals("-")) return null;
            return new BigDecimal(value.replaceAll(",", "")).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private FinancialsData fromMock(String ticker) {
        var mock = mockFallback.fetchQuarterlyFinancials(ticker);
        var mockDaily = mockFallback.fetchDailyMetrics(ticker);
        return FinancialsData.builder()
            .peRatio(mockDaily.getPeRatio())
            .pbRatio(mockDaily.getPbRatio())
            .debtToEquity(mockDaily.getDebtToEquity())
            .dividendYield(mockDaily.getDividendYield())
            .revenue(mock.getRevenue())
            .netProfit(mock.getNetProfit())
            .operatingProfit(mock.getOperatingProfit())
            .cashFlowFromOperations(mock.getCashFlowFromOperations())
            .promoterHolding(mock.getPromoterHolding())
            .promoterPledging(mock.getPromoterPledging())
            .auditorName(mock.getAuditorName())
            .build();
    }

    @Getter @Setter @Builder
    public static class FinancialsData {
        private BigDecimal peRatio;
        private BigDecimal pbRatio;
        private BigDecimal debtToEquity;
        private BigDecimal dividendYield;
        private BigDecimal revenue;
        private BigDecimal netProfit;
        private BigDecimal operatingProfit;
        private BigDecimal cashFlowFromOperations;
        private BigDecimal promoterHolding;
        private BigDecimal promoterPledging;
        private String auditorName;
    }
}

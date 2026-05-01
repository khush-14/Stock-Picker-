package com.stockpicker.api.service;

import com.stockpicker.common.entity.AuditFlag;
import com.stockpicker.common.entity.Stock;
import com.stockpicker.common.enums.FlagType;
import com.stockpicker.common.enums.Severity;
import com.stockpicker.common.repository.AuditFlagRepository;
import com.stockpicker.common.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parses MoneyControl RSS feed and checks for fraud/probe/SEBI keywords
 * associated with tracked stocks. Generates HIGH severity AuditFlags.
 *
 * RSS Feed: https://www.moneycontrol.com/rss/MCXML_V_20.xml
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsSentimentService {

    private static final String RSS_URL = "https://www.moneycontrol.com/rss/MCXML_V_20.xml";
    private static final Set<String> FRAUD_KEYWORDS = Set.of(
        "fraud", "probe", "sebi", "scam", "investigation", "money laundering",
        "default", "npa", "forensic audit", "whistleblower", "regulatory action",
        "penalty", "ban", "manipulation", "insider trading"
    );
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final StockRepository stockRepository;
    private final AuditFlagRepository auditFlagRepository;

    /**
     * Check MoneyControl RSS feed for fraud-related news about a specific ticker.
     */
    public List<AuditFlag> checkForFraudNews(String ticker) {
        Stock stock = stockRepository.findById(ticker).orElse(null);
        if (stock == null) return List.of();

        String companyName = stock.getCompanyName().toLowerCase();
        String tickerLower = ticker.toLowerCase();

        List<AuditFlag> flags = new ArrayList<>();

        try {
            List<RssItem> items = fetchRssFeed();

            for (RssItem item : items) {
                String combined = (item.title + " " + item.description).toLowerCase();

                // Check if the news mentions this stock
                boolean mentionsStock = combined.contains(tickerLower)
                    || combined.contains(companyName)
                    || containsPartialMatch(combined, companyName);

                if (!mentionsStock) continue;

                // Check for fraud keywords
                List<String> matchedKeywords = FRAUD_KEYWORDS.stream()
                    .filter(combined::contains)
                    .toList();

                if (!matchedKeywords.isEmpty()) {
                    String flagQuarter = "NEWS-" + java.time.LocalDate.now();

                    if (!auditFlagRepository.existsByTickerAndFlagTypeAndQuarter(
                            ticker, FlagType.CUSTOM, flagQuarter)) {
                        flags.add(AuditFlag.builder()
                            .ticker(ticker)
                            .flagType(FlagType.CUSTOM)
                            .severity(Severity.HIGH)
                            .description(String.format(
                                "NEWS ALERT: '%s' — Keywords: %s (Source: MoneyControl)",
                                item.title, matchedKeywords))
                            .quarter(flagQuarter)
                            .flaggedAt(Instant.now())
                            .resolved(false)
                            .build());

                        log.warn("Fraud news detected for {}: {}", ticker, item.title);
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Failed to fetch/parse MoneyControl RSS for {}: {}", ticker, e.getMessage());
        }

        return flags;
    }

    /**
     * Fetch and scan RSS feed for ALL tracked stocks.
     */
    public int scanAllStocks() {
        List<Stock> stocks = stockRepository.findAll();
        int totalFlags = 0;

        for (Stock stock : stocks) {
            try {
                List<AuditFlag> flags = checkForFraudNews(stock.getTicker());
                if (!flags.isEmpty()) {
                    auditFlagRepository.saveAll(flags);
                    totalFlags += flags.size();
                }
            } catch (Exception e) {
                log.warn("Sentiment scan failed for {}: {}", stock.getTicker(), e.getMessage());
            }
        }

        log.info("News sentiment scan complete: {} flags across {} stocks", totalFlags, stocks.size());
        return totalFlags;
    }

    private List<RssItem> fetchRssFeed() throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(RSS_URL))
            .header("User-Agent", "StockPicker/1.0")
            .timeout(TIMEOUT)
            .GET()
            .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new RuntimeException("RSS feed returned status " + response.statusCode());
        }

        return parseRss(response.body());
    }

    private List<RssItem> parseRss(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Disable external entities for security
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);

        NodeList items = doc.getElementsByTagName("item");
        List<RssItem> result = new ArrayList<>();

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String title = getTagContent(item, "title");
            String description = getTagContent(item, "description");
            String link = getTagContent(item, "link");

            result.add(new RssItem(title, description, link));
        }

        log.debug("Parsed {} RSS items from MoneyControl", result.size());
        return result;
    }

    private String getTagContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return "";
    }

    private boolean containsPartialMatch(String text, String companyName) {
        // Match first word of company name (e.g., "Reliance" from "Reliance Industries")
        String[] words = companyName.split("\\s+");
        if (words.length > 0 && words[0].length() >= 4) {
            return text.contains(words[0]);
        }
        return false;
    }

    private record RssItem(String title, String description, String link) {}
}

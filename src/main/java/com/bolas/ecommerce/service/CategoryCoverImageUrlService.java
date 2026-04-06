package com.bolas.ecommerce.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Les liens pin.it ou pages Pinterest ne sont pas des URLs d'image : le navigateur ne peut pas
 * les afficher dans {@code <img src>}. On tente d'extraire l'URL Open Graph (souvent i.pinimg.com).
 */
@Service
public class CategoryCoverImageUrlService {

    public enum ResolutionKind {
        /** Pas de traitement ou déjà une URL directe */
        DIRECT,
        /** Remplacé par l'URL og:image */
        CONVERTED_FROM_PINTEREST,
        /** Lien Pinterest mais extraction impossible (403, HTML changé, etc.) */
        PINTEREST_UNRESOLVED
    }

    public record Resolution(String url, ResolutionKind kind) {}

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    private static final Pattern OG_IMAGE_CONTENT_FIRST = Pattern.compile(
            "<meta\\s+[^>]*property=[\"']og:image[\"'][^>]*content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern OG_IMAGE_PROPERTY_FIRST = Pattern.compile(
            "<meta\\s+[^>]*content=[\"']([^\"']+)[\"'][^>]*property=[\"']og:image[\"']",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public Resolution resolveCoverUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Resolution("", ResolutionKind.DIRECT);
        }
        String url = raw.trim();
        if (isDirectPinterestCdn(url)) {
            return new Resolution(decodeHtmlEntities(url), ResolutionKind.DIRECT);
        }
        if (!isPinterestOrShortLink(url)) {
            return new Resolution(url, ResolutionKind.DIRECT);
        }
        Optional<String> og = fetchOgImageUrl(url);
        if (og.isPresent()) {
            return new Resolution(decodeHtmlEntities(og.get()), ResolutionKind.CONVERTED_FROM_PINTEREST);
        }
        return new Resolution(url, ResolutionKind.PINTEREST_UNRESOLVED);
    }

    private static boolean isDirectPinterestCdn(String url) {
        String lower = url.toLowerCase();
        return lower.contains("i.pinimg.com") || lower.contains("pinimg.com/originals");
    }

    private static boolean isPinterestOrShortLink(String url) {
        String lower = url.toLowerCase();
        return lower.contains("pin.it/") || lower.contains("pinterest.com/") || lower.contains("pinterest.fr/");
    }

    private Optional<String> fetchOgImageUrl(String pageUrl) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(pageUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8")
                    .GET()
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 400) {
                return Optional.empty();
            }
            String html = resp.body();
            if (html == null || html.isEmpty()) {
                return Optional.empty();
            }
            return extractOgImage(html);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<String> extractOgImage(String html) {
        Matcher m = OG_IMAGE_CONTENT_FIRST.matcher(html);
        if (m.find()) {
            return Optional.ofNullable(sanitizeUrl(m.group(1)));
        }
        m = OG_IMAGE_PROPERTY_FIRST.matcher(html);
        if (m.find()) {
            return Optional.ofNullable(sanitizeUrl(m.group(1)));
        }
        return Optional.empty();
    }

    private static String sanitizeUrl(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String decodeHtmlEntities(String url) {
        return url.replace("&amp;", "&");
    }
}

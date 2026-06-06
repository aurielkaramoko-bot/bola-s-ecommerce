package com.bolas.ecommerce.util;

import java.util.*;

/**
 * Mapping des 54 pays africains avec devise ISO et drapeau emoji.
 */
public final class AfricanCountryUtil {

    public record CountryInfo(String name, String currency, String flag) {}

    /** Clé = code pays ISO 2 (ex: "TG"), valeur = infos */
    private static final Map<String, CountryInfo> COUNTRIES = new LinkedHashMap<>();

    static {
        // Zone XOF
        add("TG", "Togo",               "XOF", "🇹🇬");
        add("CI", "Côte d'Ivoire",       "XOF", "🇨🇮");
        add("SN", "Sénégal",             "XOF", "🇸🇳");
        add("BJ", "Bénin",               "XOF", "🇧🇯");
        add("ML", "Mali",                "XOF", "🇲🇱");
        add("BF", "Burkina Faso",        "XOF", "🇧🇫");
        add("NE", "Niger",               "XOF", "🇳🇪");
        add("GW", "Guinée-Bissau",       "XOF", "🇬🇼");
        // Zone XAF
        add("CM", "Cameroun",            "XAF", "🇨🇲");
        add("GA", "Gabon",               "XAF", "🇬🇦");
        add("CG", "Congo",               "XAF", "🇨🇬");
        add("CD", "RD Congo",            "XAF", "🇨🇩");
        add("CF", "Centrafrique",        "XAF", "🇨🇫");
        add("TD", "Tchad",               "XAF", "🇹🇩");
        add("GQ", "Guinée Équatoriale",  "XAF", "🇬🇶");
        // Autres pays africains
        add("NG", "Nigeria",             "NGN", "🇳🇬");
        add("GH", "Ghana",               "GHS", "🇬🇭");
        add("DZ", "Algérie",             "DZD", "🇩🇿");
        add("MA", "Maroc",               "MAD", "🇲🇦");
        add("TN", "Tunisie",             "TND", "🇹🇳");
        add("EG", "Égypte",              "EGP", "🇪🇬");
        add("ZA", "Afrique du Sud",      "ZAR", "🇿🇦");
        add("KE", "Kenya",               "KES", "🇰🇪");
        add("ET", "Éthiopie",            "ETB", "🇪🇹");
        add("AO", "Angola",              "AOA", "🇦🇴");
        add("TZ", "Tanzanie",            "TZS", "🇹🇿");
        add("UG", "Ouganda",             "UGX", "🇺🇬");
        add("RW", "Rwanda",              "RWF", "🇷🇼");
        add("ZM", "Zambie",              "ZMW", "🇿🇲");
        add("ZW", "Zimbabwe",            "ZWL", "🇿🇼");
        add("MZ", "Mozambique",          "MZN", "🇲🇿");
        add("MG", "Madagascar",          "MGA", "🇲🇬");
        add("MU", "Maurice",             "MUR", "🇲🇺");
        add("SC", "Seychelles",          "SCR", "🇸🇨");
        add("NA", "Namibie",             "NAD", "🇳🇦");
        add("BW", "Botswana",            "BWP", "🇧🇼");
        add("GN", "Guinée",              "GNF", "🇬🇳");
        add("SL", "Sierra Leone",        "SLL", "🇸🇱");
        add("LR", "Liberia",             "LRD", "🇱🇷");
        add("GM", "Gambie",              "GMD", "🇬🇲");
        add("CV", "Cap-Vert",            "CVE", "🇨🇻");
        add("ST", "São Tomé-et-Príncipe","STN", "🇸🇹");
        add("KM", "Comores",             "KMF", "🇰🇲");
        add("DJ", "Djibouti",            "DJF", "🇩🇯");
        add("ER", "Érythrée",            "ERN", "🇪🇷");
        add("SO", "Somalie",             "SOS", "🇸🇴");
        add("SD", "Soudan",              "SDG", "🇸🇩");
        add("SS", "Soudan du Sud",       "SSP", "🇸🇸");
        add("BI", "Burundi",             "BIF", "🇧🇮");
        add("MW", "Malawi",              "MWK", "🇲🇼");
        add("LS", "Lesotho",             "LSL", "🇱🇸");
        add("SZ", "Eswatini",            "SZL", "🇸🇿");
        add("MR", "Mauritanie",          "MRU", "🇲🇷");
        add("LY", "Libye",               "LYD", "🇱🇾");
    }

    private static void add(String code, String name, String currency, String flag) {
        COUNTRIES.put(code, new CountryInfo(name, currency, flag));
    }

    public static Map<String, CountryInfo> all() {
        return Collections.unmodifiableMap(COUNTRIES);
    }

    public static Optional<CountryInfo> get(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(COUNTRIES.get(code.toUpperCase()));
    }

    /** Renvoie "🇹🇬 Togo" ou "" si inconnu */
    public static String flagAndName(String code) {
        return get(code).map(c -> c.flag() + " " + c.name()).orElse("");
    }

    /** Renvoie la devise ISO (ex: "XOF") ou "XOF" par défaut */
    public static String currency(String code) {
        return get(code).map(CountryInfo::currency).orElse("XOF");
    }

    private AfricanCountryUtil() {}
}

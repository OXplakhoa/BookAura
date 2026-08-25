package com.bookaura.recommendation;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Small, transparent vocabulary bridge for common collection themes. Unknown themes remain exact
 * matches, so librarians can create arbitrary categories/tags without hidden behavior.
 */
final class ThemeAliases {

    private static final Map<String, Set<String>> ALIASES = Map.ofEntries(
            Map.entry("philosophy", Set.of("philosophy", "philosophical", "reflective", "meditative", "essays")),
            Map.entry("romance", Set.of("romance", "romantic", "love", "relationship", "slow-burn", "passion")),
            Map.entry("adventure", Set.of("adventure", "adventurous", "quest", "journey", "exploration")),
            Map.entry("dark", Set.of("dark", "dystopia", "tragedy", "horror", "gothic", "noir")),
            Map.entry("comedy", Set.of("comedy", "funny", "humor", "humour", "satire", "witty", "absurd")),
            Map.entry("fiction", Set.of("fiction", "literary", "novel")),
            Map.entry("inspiring", Set.of("inspiring", "inspiration", "uplifting", "triumph", "self-improvement")),
            Map.entry("psychology", Set.of("psychology", "psychological", "mindfulness", "reflective")),
            Map.entry("science fiction", Set.of("science fiction", "sci-fi", "scifi", "space opera")),
            Map.entry("self-help", Set.of("self-help", "self improvement", "self-improvement", "productivity")),
            Map.entry("history", Set.of("history", "historical", "historical fiction")),
            Map.entry("horror", Set.of("horror", "gothic", "terrifying")),
            Map.entry("thriller", Set.of("thriller", "suspense", "mystery", "noir"))
    );

    private ThemeAliases() {
    }

    static Set<String> forTheme(String theme) {
        return ALIASES.getOrDefault(theme.trim().toLowerCase(Locale.ROOT), Set.of(theme));
    }
}

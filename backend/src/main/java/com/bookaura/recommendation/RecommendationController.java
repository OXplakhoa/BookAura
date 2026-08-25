package com.bookaura.recommendation;

import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Public Shelf Aura endpoint (P2 #36). Deterministic rule-based scoring; top 6 by default.
 */
@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "Recommendations", description = "Public Shelf Aura reading recommendations")
public class RecommendationController {

    static final int DEFAULT_LIMIT = 6;
    private static final int MIN_TIME_MINUTES = 15;
    private static final int MAX_TIME_MINUTES = 24 * 60;

    private final RecommendationEngine engine;

    public RecommendationController(RecommendationEngine engine) {
        this.engine = engine;
    }

    @Operation(summary = "Score active books against mood/time/theme/intensity signals (top 6, deterministic)")
    @GetMapping("/aura")
    public List<AuraRecommendation> aura(@RequestParam(required = false) String moods,
                                         @RequestParam(required = false) Integer timeMinutes,
                                         @RequestParam(required = false) String themes,
                                         @RequestParam(required = false) String intensity) {
        AuraQuery query = new AuraQuery(parseMoods(moods), parseTime(timeMinutes),
                parseCsv(themes), parseIntensity(intensity));
        if (!query.hasSignals()) {
            throw new BusinessException(ErrorCode.AURA_INVALID_PARAM,
                    "Pick at least one mood or theme to read your aura");
        }
        return engine.recommend(query, DEFAULT_LIMIT);
    }

    private Set<Mood> parseMoods(String raw) {
        return parseCsv(raw).stream().map(value -> Mood.fromParam(value)
                        .orElseThrow(() -> new BusinessException(ErrorCode.AURA_INVALID_PARAM,
                                "Unknown mood: " + value + ". Valid: " + moodVocabulary())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Intensity parseIntensity(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Intensity.fromParam(raw)
                .orElseThrow(() -> new BusinessException(ErrorCode.AURA_INVALID_PARAM,
                        "Unknown intensity: " + raw + ". Valid: light, medium, deep"));
    }

    private Integer parseTime(Integer timeMinutes) {
        if (timeMinutes == null) {
            return null;
        }
        if (timeMinutes < MIN_TIME_MINUTES || timeMinutes > MAX_TIME_MINUTES) {
            throw new BusinessException(ErrorCode.AURA_INVALID_PARAM,
                    "timeMinutes must be between " + MIN_TIME_MINUTES + " and " + MAX_TIME_MINUTES);
        }
        return timeMinutes;
    }

    private static Set<String> parseCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String moodVocabulary() {
        return Arrays.stream(Mood.values())
                .map(m -> m.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
    }
}

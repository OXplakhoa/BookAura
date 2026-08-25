package com.bookaura.recommendation;

/**
 * Exact contribution of each selected signal to one rule-based recommendation's score.
 * The experimental embedding engine returns all-zero values here because semantic similarity is
 * not an additive mood/theme/time/intensity rule-point calculation.
 */
public record AuraScoreBreakdown(
        int mood,
        int theme,
        int time,
        int intensity
) {
}

package com.bookaura.recommendation;

/** Exact contribution of each selected signal to one recommendation's total score. */
public record AuraScoreBreakdown(
        int mood,
        int theme,
        int time,
        int intensity
) {
}

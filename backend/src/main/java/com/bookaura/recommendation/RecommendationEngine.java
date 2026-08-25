package com.bookaura.recommendation;

import java.util.List;

/**
 * Pluggable recommender seam. RuleBasedRecommendationEngine is the deterministic default (P2 #36);
 * EmbeddingRecommendationEngine is an opt-in, offline experimental implementation (P2 #39).
 */
public interface RecommendationEngine {

    /** Returns the top {@code limit} highest-scored books for the query, deterministically ordered. */
    List<AuraRecommendation> recommend(AuraQuery query, int limit);
}

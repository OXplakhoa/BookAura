package com.bookaura.recommendation;

import java.util.List;

/**
 * Pluggable recommender seam. RuleBasedRecommendationEngine is the deterministic default (P2 #36);
 * an EmbeddingRecommendationEngine stub may later sit behind the same interface + feature flag (P2 #39).
 */
public interface RecommendationEngine {

    /** Returns the top {@code limit} highest-scored books for the query, deterministically ordered. */
    List<AuraRecommendation> recommend(AuraQuery query, int limit);
}

package com.bookaura.recommendation;

import com.bookaura.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationEngineSelectionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RecommendationEngine engine;

    @Test
    void defaultPropertySelectsRuleBasedEngine() {
        assertThat(engine).isInstanceOf(RuleBasedRecommendationEngine.class);
    }
}

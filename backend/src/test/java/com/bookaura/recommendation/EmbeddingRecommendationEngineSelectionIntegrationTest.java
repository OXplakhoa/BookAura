package com.bookaura.recommendation;

import com.bookaura.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "bookaura.recommendation.engine=embedding")
class EmbeddingRecommendationEngineSelectionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RecommendationEngine engine;

    @Test
    void embeddingPropertySelectsExperimentalEngine() {
        assertThat(engine).isInstanceOf(EmbeddingRecommendationEngine.class);
    }
}

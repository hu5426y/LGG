package com.ljj.campusrun.run;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RunMetricCalculatorTest {

    @Test
    void shouldCalculateAveragePace() {
        assertEquals(360, RunMetricCalculator.averagePaceSeconds(5.0, 1800));
    }

    @Test
    void shouldReturnZeroWhenDistanceIsInvalid() {
        assertEquals(0, RunMetricCalculator.averagePaceSeconds(0, 1000));
        assertEquals(0, RunMetricCalculator.calories(0));
    }

    @Test
    void shouldEstimateCalories() {
        assertEquals(300, RunMetricCalculator.calories(5.0));
    }
}

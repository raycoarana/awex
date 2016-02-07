package com.raycoarana.awex;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public class BasePerf extends AbstractBenchmark {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

}

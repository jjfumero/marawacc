package uk.ac.ed.jpai.test.gpu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.stream.IntStream;

import org.junit.Test;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.jpai.MapAccelerator;
import uk.ac.ed.jpai.test.base.MarawaccOpenCLTestBase;

public class TestScopeArrays extends MarawaccOpenCLTestBase {

    @Test
    public void scopeTest() {

        final int size = 100;

        float[] inreal = new float[size];

        IntStream.range(0, size).parallel().forEach(i -> {
            inreal[i] = 0.01f;
        });

        PArray<Integer> input = new PArray<>(size, TypeFactory.Integer());
        for (int i = 0; i < size; i++) {
            input.put(i, i);
        }

        MapAccelerator<Integer, Float> function = new MapAccelerator<>(idx -> {
            float r = inreal[idx] * idx;
            return r;
        });

        PArray<Float> result = function.apply(input);

        assertNotNull(result);

        for (int i = 0; i < size; i++) {
            assertEquals(inreal[i] * i, result.get(i), 0.1);
        }

    }

}

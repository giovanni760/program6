package edu.psp.program6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GammaTest {

    @Test
    void gammaOfOneIsOne() {
        assertEquals(1.0, Gamma.of(1), 1e-12);
    }

    @Test
    void gammaOfHalfIsSqrtPi() {
        assertEquals(Math.sqrt(Math.PI), Gamma.of(0.5), 1e-12);
    }

    @Test
    void gammaOfIntegerIsFactorial() {
        assertEquals(24.0, Gamma.of(5), 1e-12); // 4!
    }

    @Test
    void gammaOfNineHalvesMatchesKit() {
        assertEquals(11.63173, Gamma.of(4.5), 1e-5);
    }

    @Test
    void rejectsNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> Gamma.of(0));
        assertThrows(IllegalArgumentException.class, () -> Gamma.of(-1.5));
    }

    @Test
    void rejectsNonHalfInteger() {
        assertThrows(IllegalArgumentException.class, () -> Gamma.of(2.3));
    }

    @Test
    void largestRepresentableValues() {
        assertEquals(9.48337e307, Gamma.of(171.5), 1e302);
        assertThrows(IllegalArgumentException.class, () -> Gamma.of(172));
    }

    @Test
    void rejectsNaN() {
        assertThrows(IllegalArgumentException.class, () -> Gamma.of(Double.NaN));
    }

    @ParameterizedTest(name = "Gamma({0}) recursiva = iterativa")
    @ValueSource(doubles = {0.5, 1, 1.5, 2, 4.5, 5, 10, 15.5, 171.5})
    void recursiveMatchesIterative(double x) {
        double expected = Gamma.of(x);
        assertEquals(expected, Gamma.ofRecursive(x), Math.abs(expected) * 1e-12);
    }

    @Test
    void recursiveRejectsSameInputs() {
        assertThrows(IllegalArgumentException.class, () -> Gamma.ofRecursive(0));
        assertThrows(IllegalArgumentException.class, () -> Gamma.ofRecursive(2.3));
        assertThrows(IllegalArgumentException.class, () -> Gamma.ofRecursive(172));
    }

    @ParameterizedTest(name = "ln Gamma({0})")
    @ValueSource(doubles = {0.5, 1, 1.5, 2, 4.5, 5, 30, 171.5})
    void logMatchesGamma(double x) {
        assertEquals(Math.log(Gamma.of(x)), Gamma.logOf(x), 1e-9);
    }

    @Test
    void logWorksBeyondDoubleRange() {
        // ln(171!) + ln(172) = ln(172!)
        assertEquals(Gamma.logOf(172) + Math.log(172), Gamma.logOf(173), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> Gamma.logOf(-1));
        assertThrows(IllegalArgumentException.class, () -> Gamma.logOf(1.3));
    }

    @Test
    void ratioMatchesDivision() {
        assertEquals(Gamma.of(5) / Gamma.of(4.5), Gamma.ratio(5, 4.5), 1e-9);
        // Gamma(x+1)/Gamma(x) = x, incluso con valores que se desbordan
        assertEquals(500.0, Gamma.ratio(501, 500), 1e-6);
    }

    @Test
    void factorial() {
        assertEquals(1.0, Gamma.factorial(0), 0);
        assertEquals(1.0, Gamma.factorial(1), 0);
        assertEquals(120.0, Gamma.factorial(5), 0);
        assertTrue(Double.isFinite(Gamma.factorial(Gamma.MAX_FACTORIAL)));
        assertThrows(IllegalArgumentException.class, () -> Gamma.factorial(-1));
        assertThrows(IllegalArgumentException.class, () -> Gamma.factorial(Gamma.MAX_FACTORIAL + 1));
    }

    @Test
    void integerAndHalfIntegerChecks() {
        assertTrue(Gamma.isInteger(3));
        assertFalse(Gamma.isInteger(3.5));
        assertTrue(Gamma.isHalfInteger(3.5));
        assertFalse(Gamma.isHalfInteger(3));
        assertFalse(Gamma.isHalfInteger(3.25));
    }
}

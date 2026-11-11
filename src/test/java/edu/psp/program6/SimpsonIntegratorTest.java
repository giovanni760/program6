package edu.psp.program6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SimpsonIntegratorTest {

    private final SimpsonIntegrator integrator = new SimpsonIntegrator();

    /** Tabla 1 del Assignment Kit. */
    @ParameterizedTest(name = "0 a x={0}, dof={1} -> p={2}")
    @CsvSource({
            "1.1,    9,  0.35006",
            "1.1812, 10, 0.36757",
            "2.750,  30, 0.49500"
    })
    void table1(double x, int dof, double expected) {
        double p = integrator.integrate(new TDistribution(dof), x);
        assertEquals(expected, p, 0.00001);
    }

    @Test
    void singlePassWithTenSegmentsMatchesTable2() {
        double p = SimpsonIntegrator.simpson(new TDistribution(9), 1.1, 10);
        assertEquals(0.3500589, p, 1e-6);
    }

    @Test
    void triangleExampleFromKit() {
        // F(x) = 2x de 0 a 4, num_seg = 4 -> 16
        assertEquals(16.0, SimpsonIntegrator.simpson(v -> 2 * v, 4, 4), 1e-12);
        assertEquals(16.0, integrator.integrate(v -> 2 * v, 4), 1e-9);
    }

    @Test
    void integralFromZeroToZeroIsZero() {
        assertEquals(0.0, integrator.integrate(new TDistribution(9), 0.0), 1e-12);
    }

    @Test
    void customSegmentsAndError() {
        SimpsonIntegrator custom = new SimpsonIntegrator(4, 1e-8);
        assertEquals(0.35006, custom.integrate(new TDistribution(9), 1.1), 1e-5);
    }

    @Test
    void rejectsInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> new SimpsonIntegrator(3, 1e-5));
        assertThrows(IllegalArgumentException.class, () -> new SimpsonIntegrator(0, 1e-5));
        assertThrows(IllegalArgumentException.class, () -> new SimpsonIntegrator(-2, 1e-5));
        assertThrows(IllegalArgumentException.class, () -> new SimpsonIntegrator(10, 0));
        assertThrows(IllegalArgumentException.class, () -> new SimpsonIntegrator(10, -1e-5));
    }

    @Test
    void rejectsNonFiniteX() {
        TDistribution t = new TDistribution(9);
        assertThrows(IllegalArgumentException.class, () -> integrator.integrate(t, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> integrator.integrate(t, Double.POSITIVE_INFINITY));
    }

    @Test
    void rejectsNaNError() {
        assertThrows(IllegalArgumentException.class, () -> new SimpsonIntegrator(10, Double.NaN));
    }

    @Test
    void defaultsAreExposed() {
        assertEquals(SimpsonIntegrator.DEFAULT_NUM_SEG, integrator.getInitialNumSeg());
        assertEquals(SimpsonIntegrator.DEFAULT_ERROR, integrator.getError(), 0);
    }

    @Test
    void integratesBetweenArbitraryLimits() {
        // x^2 de 1 a 3 = 26/3 (Simpson es exacto para polinomios de grado <= 3)
        assertEquals(26.0 / 3.0, integrator.integrate(v -> v * v, 1, 3), 1e-12);
        assertEquals(26.0 / 3.0, SimpsonIntegrator.simpson(v -> v * v, 1, 3, 2), 1e-12);
        // Invertir los limites cambia el signo
        assertEquals(-26.0 / 3.0, integrator.integrate(v -> v * v, 3, 1), 1e-12);
    }

    @Test
    void tDistributionIsSymmetric() {
        TDistribution t = new TDistribution(9);
        double right = integrator.integrate(t, 0, 1.1);
        double both = integrator.integrate(t, -1.1, 1.1);
        assertEquals(2 * right, both, 1e-5);
    }

    @Test
    void detailsReportConvergence() {
        SimpsonIntegrator.Result r = integrator.integrateWithDetails(new TDistribution(9), 0, 1.1);
        assertTrue(r.converged());
        assertTrue(r.difference() < SimpsonIntegrator.DEFAULT_ERROR);
        assertEquals(SimpsonIntegrator.DEFAULT_NUM_SEG << r.iterations(), r.numSeg());
        assertEquals(0.35006, r.value(), 0.00001);
    }

    @Test
    void stopsWhenItNeverConverges() {
        SimpsonIntegrator.Result r = integrator.integrateWithDetails(v -> Double.NaN, 0, 1);
        assertFalse(r.converged());
        assertTrue(Double.isNaN(r.value()));
        assertTrue(r.numSeg() >= 1 << 24);
    }

    @Test
    void singlePassRejectsInvalidSegments() {
        assertThrows(IllegalArgumentException.class, () -> SimpsonIntegrator.simpson(v -> v, 1, 3));
        assertThrows(IllegalArgumentException.class, () -> SimpsonIntegrator.simpson(v -> v, 1, 0));
    }

    @Test
    void rejectsNonFiniteLowerLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> integrator.integrate(v -> v, Double.NEGATIVE_INFINITY, 1));
    }
}

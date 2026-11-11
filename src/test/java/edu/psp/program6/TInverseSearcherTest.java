package edu.psp.program6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TInverseSearcherTest {

    private final TInverseSearcher searcher = new TInverseSearcher();

    /** Tabla 1 del Assignment Kit: p, dof y valor esperado de x. */
    @ParameterizedTest(name = "p={0}, dof={1} -> x={2}")
    @CsvSource({
            "0.20,  6,  0.55338",
            "0.45,  15, 1.75305",
            "0.495, 4,  4.60409"
    })
    void table1(double p, int dof, double expected) {
        TInverseSearcher.Result r = searcher.search(p, dof);
        assertTrue(r.converged());
        assertEquals(expected, r.x(), 2e-5);
    }

    @ParameterizedTest(name = "la integral de 0 a x vuelve a dar p={0}")
    @ValueSource(doubles = {0.01, 0.1, 0.25, 0.4, 0.499})
    void integralOfFoundXGivesBackP(double p) {
        TInverseSearcher.Result r = searcher.search(p, 9);
        assertTrue(r.converged());
        assertEquals(p, r.p(), 0);
        assertEquals(p, r.integral(), TInverseSearcher.DEFAULT_ERROR);
        double check = new SimpsonIntegrator().integrate(new TDistribution(9), r.x());
        assertEquals(p, check, 1e-5);
    }

    @Test
    void kitErrorConvergesButIsOnlyAccurateToThreeDecimals() {
        TInverseSearcher kit = new TInverseSearcher(
                new SimpsonIntegrator(SimpsonIntegrator.DEFAULT_NUM_SEG, TInverseSearcher.KIT_ERROR),
                TInverseSearcher.DEFAULT_START, TInverseSearcher.DEFAULT_STEP,
                TInverseSearcher.KIT_ERROR, TInverseSearcher.DEFAULT_MAX_TRIALS);
        TInverseSearcher.Result r = kit.search(0.495, 4);
        assertTrue(r.converged());
        assertTrue(Math.abs(r.integral() - 0.495) < TInverseSearcher.KIT_ERROR);
        assertEquals(4.60409, r.x(), 0.005);
        // Con el error fino se llega mas cerca del valor del kit
        assertTrue(Math.abs(searcher.search(0.495, 4).x() - 4.60409)
                < Math.abs(r.x() - 4.60409));
    }

    @Test
    void firstTrialStartsAtOneWithStepHalf() {
        TInverseSearcher.Result r = searcher.search(0.45, 15);
        TInverseSearcher.Trial first = r.history().get(0);
        assertEquals(1, first.number());
        assertEquals(1.0, first.x(), 0);
        assertEquals(0.5, first.step(), 0);
        assertEquals(first.integral() - 0.45, first.error(), 1e-12);
    }

    @Test
    void historyHasOneTrialPerAttemptInOrder() {
        TInverseSearcher.Result r = searcher.search(0.2, 6);
        List<TInverseSearcher.Trial> history = r.history();
        assertEquals(r.trials(), history.size());
        for (int i = 0; i < history.size(); i++) {
            assertEquals(i + 1, history.get(i).number());
        }
        TInverseSearcher.Trial last = history.get(history.size() - 1);
        assertEquals(last.x(), r.x(), 0);
        assertEquals(last.integral(), r.integral(), 0);
    }

    @Test
    void historyCannotBeModified() {
        List<TInverseSearcher.Trial> history = searcher.search(0.2, 6).history();
        TInverseSearcher.Trial first = history.get(0);
        assertThrows(UnsupportedOperationException.class, () -> history.add(first));
        assertThrows(UnsupportedOperationException.class, () -> history.remove(0));
    }

    /** Regla del kit: d se queda igual mientras el signo no cambie y se divide entre 2 cuando cambia. */
    @ParameterizedTest(name = "regla de ajuste de d: p={0}, dof={1}")
    @CsvSource({
            "0.20,  6",
            "0.45,  15",
            "0.495, 4"
    })
    void stepIsHalvedOnlyWhenTheSignOfTheErrorChanges(double p, int dof) {
        List<TInverseSearcher.Trial> history = searcher.search(p, dof).history();
        for (int i = 1; i < history.size(); i++) {
            TInverseSearcher.Trial before = history.get(i - 1);
            TInverseSearcher.Trial now = history.get(i);
            boolean signChanged = i >= 2 && Math.signum(before.error()) != Math.signum(history.get(i - 2).error());
            double expectedStep = signChanged ? before.step() / 2.0 : before.step();
            assertEquals(expectedStep, now.step(), 1e-15);
        }
    }

    @Test
    void movesUpWhenTheIntegralIsTooLowAndDownWhenTooHigh() {
        List<TInverseSearcher.Trial> history = searcher.search(0.45, 15).history();
        for (int i = 0; i < history.size() - 1; i++) {
            TInverseSearcher.Trial now = history.get(i);
            TInverseSearcher.Trial next = history.get(i + 1);
            if (now.error() < 0) {
                assertTrue(next.x() > now.x());
            } else {
                assertTrue(next.x() < now.x());
            }
        }
    }

    @Test
    void trialValueCanBecomeExactlyZero() {
        // Con p muy pequeno x pasa por 1.0 -> 0.5 -> 0.0 y la integral de 0 a 0 vale 0
        TInverseSearcher.Result r = searcher.search(0.0001, 9);
        assertTrue(r.converged());
        TInverseSearcher.Trial third = r.history().get(2);
        assertEquals(0.0, third.x(), 0);
        assertEquals(0.0, third.integral(), 0);
        assertEquals(-0.0001, third.error(), 1e-12);
        assertEquals(0.0001 / 0.38803, r.x(), 1e-4);
    }

    @Test
    void worksWithAnyIncreasingFunction() {
        // La integral de la funcion constante 1 de 0 a x vale x, asi que x = p
        TInverseSearcher.Result r = searcher.search(0.3, v -> 1.0);
        assertTrue(r.converged());
        assertEquals(0.3, r.x(), 1e-6);
    }

    @Test
    void givesUpAfterMaxTrials() {
        TInverseSearcher limited = new TInverseSearcher(new SimpsonIntegrator(),
                TInverseSearcher.DEFAULT_START, TInverseSearcher.DEFAULT_STEP, TInverseSearcher.DEFAULT_ERROR, 3);
        TInverseSearcher.Result r = limited.search(0.495, 4);
        assertFalse(r.converged());
        assertEquals(3, r.trials());
        assertEquals(3, r.history().size());
        assertTrue(Math.abs(r.integral() - 0.495) >= TInverseSearcher.DEFAULT_ERROR);
    }

    @Test
    void largeTCurveNearTheLimitNeedsManyTrialsButConverges() {
        TInverseSearcher.Result r = searcher.search(0.4999, 4);
        assertTrue(r.converged());
        assertTrue(r.trials() > 20);
        assertTrue(r.x() > 10);
    }

    @ParameterizedTest(name = "p={0} esta fuera de rango")
    @ValueSource(doubles = {0.0, -0.1, 0.5, 0.75, 1.0, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsPOutOfRange(double p) {
        assertThrows(IllegalArgumentException.class, () -> searcher.search(p, 9));
        assertThrows(IllegalArgumentException.class, () -> searcher.search(p, v -> 1.0));
    }

    @Test
    void rejectsInvalidDegreesOfFreedom() {
        assertThrows(IllegalArgumentException.class, () -> searcher.search(0.3, 0));
        assertThrows(IllegalArgumentException.class, () -> searcher.search(0.3, -4));
        assertThrows(IllegalArgumentException.class, () -> searcher.search(0.3, 343));
    }

    @Test
    void rejectsInvalidConstructorArguments() {
        SimpsonIntegrator integrator = new SimpsonIntegrator();
        assertThrows(IllegalArgumentException.class, () -> new TInverseSearcher(null, 1.0, 0.5, 1e-5, 10));
        assertThrows(IllegalArgumentException.class, () -> new TInverseSearcher(integrator, Double.NaN, 0.5, 1e-5, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new TInverseSearcher(integrator, Double.POSITIVE_INFINITY, 0.5, 1e-5, 10));
        assertThrows(IllegalArgumentException.class, () -> new TInverseSearcher(integrator, 1.0, 0.0, 1e-5, 10));
        assertThrows(IllegalArgumentException.class, () -> new TInverseSearcher(integrator, 1.0, -0.5, 1e-5, 10));
        assertThrows(IllegalArgumentException.class, () -> new TInverseSearcher(integrator, 1.0, Double.NaN, 1e-5, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new TInverseSearcher(integrator, 1.0, Double.POSITIVE_INFINITY, 1e-5, 10));
        assertThrows(IllegalArgumentException.class, () -> new TInverseSearcher(integrator, 1.0, 0.5, 0.0, 10));
        assertThrows(IllegalArgumentException.class, () -> new TInverseSearcher(integrator, 1.0, 0.5, -1e-5, 10));
        assertThrows(IllegalArgumentException.class, () -> new TInverseSearcher(integrator, 1.0, 0.5, Double.NaN, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new TInverseSearcher(integrator, 1.0, 0.5, Double.POSITIVE_INFINITY, 10));
        assertThrows(IllegalArgumentException.class, () -> new TInverseSearcher(integrator, 1.0, 0.5, 1e-5, 0));
    }

    @Test
    void defaultsAreExposed() {
        assertEquals(TInverseSearcher.DEFAULT_START, searcher.getStart(), 0);
        assertEquals(TInverseSearcher.DEFAULT_STEP, searcher.getStep(), 0);
        assertEquals(TInverseSearcher.DEFAULT_ERROR, searcher.getError(), 0);
        assertEquals(TInverseSearcher.DEFAULT_MAX_TRIALS, searcher.getMaxTrials());
        assertEquals(1.0, TInverseSearcher.DEFAULT_START, 0);
        assertEquals(0.5, TInverseSearcher.DEFAULT_STEP, 0);
        assertEquals(0.00001, TInverseSearcher.KIT_ERROR, 0);
    }
}

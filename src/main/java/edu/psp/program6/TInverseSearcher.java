package edu.psp.program6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * Algoritmo de busqueda del Programa 6: encuentra el valor de x para el cual
 * la integral de la funcion t de 0 a x da como resultado p.
 *
 * <p>Sigue la tabla del Assignment Kit:
 * <ol>
 *   <li>Se empieza con un valor de prueba x = 1.0 y d = 0.5.</li>
 *   <li>Se integra de 0 a x con {@link SimpsonIntegrator} y se compara con p.</li>
 *   <li>Si la integral es muy baja se suma d a x; si es muy alta se resta d.</li>
 *   <li>Mientras el signo del error no cambie, d se queda igual.</li>
 *   <li>Cada vez que el signo del error cambia, d se divide entre 2.</li>
 *   <li>Se repite hasta que |integral - p| sea menor que el error aceptable
 *       ({@link #DEFAULT_ERROR} por omision).</li>
 * </ol>
 *
 * <p>El kit sugiere un error de 0.00001 ({@link #KIT_ERROR}). Como la curva t es casi
 * plana lejos del centro, con ese error x solo queda exacto hasta el tercer
 * decimal (por ejemplo 4.60156 en lugar de 4.60409 para p = 0.495 y dof = 4).
 * Por eso el valor por omision es mas fino y reproduce la Tabla 1 con cinco
 * decimales; el valor del kit sigue disponible para quien lo quiera usar.
 *
 * <p>Con este metodo x puede llegar a valer exactamente 0 (por ejemplo cuando
 * p es muy pequeno); {@link SimpsonIntegrator} maneja ese caso porque la
 * integral de 0 a 0 vale 0.
 *
 * <p>Como la funcion t es simetrica, la integral de 0 a infinito vale 0.5, por lo
 * que p debe estar estrictamente entre 0 y 0.5.
 */
public final class TInverseSearcher {

    public static final double DEFAULT_START = 1.0;
    public static final double DEFAULT_STEP = 0.5;
    public static final double KIT_ERROR = 0.00001;
    public static final double DEFAULT_ERROR = 0.0000001;
    public static final int DEFAULT_MAX_TRIALS = 10_000;

    /** Cota superior (excluida) de p: la mitad del area total bajo la curva t. */
    public static final double MAX_P = 0.5;

    /**
     * Un intento de la busqueda. La lista de intentos es la tabla de traza que
     * pide la revision del diseno.
     *
     * @param number   numero de intento (empieza en 1)
     * @param x        valor de prueba del limite superior
     * @param integral resultado de integrar de 0 a x
     * @param error    integral - p (positivo si la integral es muy alta)
     * @param step     valor de d vigente al evaluar este intento
     */
    public record Trial(int number, double x, double integral, double error, double step) {
    }

    /**
     * Resultado de una busqueda.
     *
     * @param x         valor de x encontrado (o el ultimo intento si no convergio)
     * @param p         valor de p que se buscaba
     * @param integral  integral de 0 a x en el ultimo intento
     * @param trials    cantidad de intentos realizados
     * @param converged true si |integral - p| quedo por debajo del error aceptable
     * @param history   todos los intentos, en orden (no modificable)
     */
    public record Result(double x, double p, double integral, int trials, boolean converged, List<Trial> history) {
    }

    private final SimpsonIntegrator integrator;
    private final double start;
    private final double step;
    private final double error;
    private final int maxTrials;

    public TInverseSearcher() {
        this(new SimpsonIntegrator(SimpsonIntegrator.DEFAULT_NUM_SEG, DEFAULT_ERROR),
                DEFAULT_START, DEFAULT_STEP, DEFAULT_ERROR, DEFAULT_MAX_TRIALS);
    }

    /**
     * @param integrator integrador que se usa en cada intento
     * @param start      valor de prueba inicial de x (finito)
     * @param step       valor inicial de d (mayor que 0)
     * @param error      error aceptable para |integral - p| (mayor que 0)
     * @param maxTrials  maximo de intentos antes de rendirse (mayor que 0)
     * @throws IllegalArgumentException si algun parametro no es valido
     */
    public TInverseSearcher(SimpsonIntegrator integrator, double start, double step, double error, int maxTrials) {
        if (integrator == null) {
            throw new IllegalArgumentException("El integrador no puede ser nulo");
        }
        if (!Double.isFinite(start)) {
            throw new IllegalArgumentException("El valor inicial debe ser un numero finito: " + start);
        }
        if (!(step > 0) || !Double.isFinite(step)) {
            throw new IllegalArgumentException("d debe ser positivo: " + step);
        }
        if (!(error > 0) || !Double.isFinite(error)) {
            throw new IllegalArgumentException("El error aceptable debe ser positivo: " + error);
        }
        if (maxTrials <= 0) {
            throw new IllegalArgumentException("El maximo de intentos debe ser mayor que 0: " + maxTrials);
        }
        this.integrator = integrator;
        this.start = start;
        this.step = step;
        this.error = error;
        this.maxTrials = maxTrials;
    }

    public double getStart() {
        return start;
    }

    public double getStep() {
        return step;
    }

    public double getError() {
        return error;
    }

    public int getMaxTrials() {
        return maxTrials;
    }

    /**
     * Busca x tal que la integral de la funcion t con {@code dof} grados de
     * libertad, de 0 a x, sea p.
     *
     * @param p   resultado deseado de la integral (0 &lt; p &lt; 0.5)
     * @param dof grados de libertad (entero positivo)
     * @return el resultado con x, el numero de intentos y la tabla de traza
     * @throws IllegalArgumentException si p esta fuera de rango o dof no es valido
     */
    public Result search(double p, int dof) {
        validateP(p);
        return search(p, new TDistribution(dof));
    }

    /**
     * Igual que {@link #search(double, int)} pero con cualquier funcion
     * creciente en x &gt; 0 (util para probar el algoritmo con funciones sencillas).
     *
     * @param p resultado deseado de la integral (0 &lt; p &lt; 0.5)
     * @param f funcion que se integra de 0 a x
     */
    public Result search(double p, DoubleUnaryOperator f) {
        validateP(p);
        List<Trial> history = new ArrayList<>();
        double x = start;
        double d = step;
        int previousSign = 0;
        double integral = 0.0;

        for (int n = 1; n <= maxTrials; n++) {
            integral = integrator.integrate(f, x);
            double difference = integral - p;
            history.add(new Trial(n, x, integral, difference, d));

            if (Math.abs(difference) < error) {
                return new Result(x, p, integral, n, true, Collections.unmodifiableList(history));
            }

            int sign = difference > 0 ? 1 : -1;
            if (previousSign != 0 && sign != previousSign) {
                d /= 2.0;
            }
            // Integral muy alta: se resta d. Integral muy baja: se suma d.
            x -= sign * d;
            previousSign = sign;
        }
        return new Result(x, p, integral, maxTrials, false, Collections.unmodifiableList(history));
    }

    private static void validateP(double p) {
        if (Double.isNaN(p) || p <= 0 || p >= MAX_P) {
            throw new IllegalArgumentException("p debe estar entre 0 y 0.5 (sin incluirlos): " + p);
        }
    }
}

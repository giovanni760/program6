package edu.psp.program6;

/**
 * Funcion gamma para valores enteros y medios enteros (n/2), segun el
 * Assignment Kit del Programa 6:
 * Gamma(x) = (x-1) * Gamma(x-1), Gamma(1) = 1, Gamma(1/2) = sqrt(pi).
 *
 * <p>Para x entero se usa Gamma(x) = (x-1)! y para x = k + 1/2 se usa
 * Gamma(x) = (x-1)(x-2)...(1/2) * sqrt(pi). Tambien se ofrece la version
 * recursiva literal del kit, el logaritmo de Gamma (que no se desborda) y el
 * cociente Gamma(a)/Gamma(b), que es lo que necesita la distribucion t.
 */
public final class Gamma {

    public static final double MAX_ARGUMENT = 171.5;

    
    public static final int MAX_FACTORIAL = 170;

    private static final double EPS = 1e-9;
    private static final double SQRT_PI = Math.sqrt(Math.PI);

    private Gamma() {
    }

    /**
     * Calcula Gamma(x) para x entero positivo o x = k + 1/2 (k >= 0).
     *
     * @param x valor positivo, entero o medio entero
     * @return Gamma(x)
     * @throws IllegalArgumentException si x no es positivo, no es multiplo de 1/2
     *                                  o Gamma(x) no cabe en un double (x > 171.5)
     */
    public static double of(double x) {
        validate(x);
        checkRange(x);
        if (isInteger(x)) {
            return factorial((int) Math.round(x) - 1);
        }
        return halfInteger(x);
    }

    /**
     * Calcula Gamma(x) aplicando literalmente la definicion recursiva del kit:
     * Gamma(x) = (x-1) * Gamma(x-1), con Gamma(1) = 1 y Gamma(1/2) = sqrt(pi).
     * Debe dar el mismo resultado que {@link #of(double)}.
     *
     * @param x valor positivo, entero o medio entero
     * @return Gamma(x)
     * @throws IllegalArgumentException en los mismos casos que {@link #of(double)}
     */
    public static double ofRecursive(double x) {
        validate(x);
        checkRange(x);
        return recurse(x);
    }

    /**
     * Calcula ln(Gamma(x)) sumando logaritmos, por lo que no se desborda aunque
     * x sea mayor que {@link #MAX_ARGUMENT}.
     *
     * @param x valor positivo, entero o medio entero
     * @return ln(Gamma(x))
     * @throws IllegalArgumentException si x no es positivo o no es multiplo de 1/2
     */
    public static double logOf(double x) {
        validate(x);
        double sum = isInteger(x) ? 0.0 : Math.log(SQRT_PI);
        double start = isInteger(x) ? 1.0 : 0.5;
        for (double c = start; c < x - EPS; c += 1.0) {
            sum += Math.log(c);
        }
        return sum;
    }

    /**
     * Calcula Gamma(a) / Gamma(b) con logaritmos para evitar desbordes cuando
     * a y b son grandes (por ejemplo Gamma((dof+1)/2) / Gamma(dof/2)).
     *
     * @param a argumento del numerador (entero o medio entero positivo)
     * @param b argumento del denominador (entero o medio entero positivo)
     * @return Gamma(a) / Gamma(b)
     * @throws IllegalArgumentException si a o b no son validos
     */
    public static double ratio(double a, double b) {
        return Math.exp(logOf(a) - logOf(b));
    }

    /**
     * Calcula n! = 1 * 2 * ... * n, con 0! = 1.
     *
     * @param n entero entre 0 y {@link #MAX_FACTORIAL}
     * @return n!
     * @throws IllegalArgumentException si n es negativo o n! no cabe en un double
     */
    public static double factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n debe ser mayor o igual a 0: " + n);
        }
        if (n > MAX_FACTORIAL) {
            throw new IllegalArgumentException(n + "! excede el rango de double");
        }
        double result = 1.0;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    /** @return true si x es un entero (con tolerancia EPS) */
    public static boolean isInteger(double x) {
        return Math.abs(x - Math.rint(x)) < EPS;
    }

    /** @return true si x = k + 1/2 para algun entero k */
    public static boolean isHalfInteger(double x) {
        return isInteger(x * 2.0) && !isInteger(x);
    }

    /** Gamma(k + 1/2) = (k - 1/2)(k - 3/2)...(1/2) * sqrt(pi). */
    private static double halfInteger(double x) {
        double result = SQRT_PI;
        for (double c = 0.5; c < x - EPS; c += 1.0) {
            result *= c;
        }
        return result;
    }

    private static double recurse(double x) {
        if (Math.abs(x - 1.0) < EPS) {
            return 1.0;
        }
        if (Math.abs(x - 0.5) < EPS) {
            return SQRT_PI;
        }
        return (x - 1.0) * recurse(x - 1.0);
    }

    private static void validate(double x) {
        if (Double.isNaN(x) || x <= 0) {
            throw new IllegalArgumentException("x debe ser positivo: " + x);
        }
        if (!isInteger(x) && !isHalfInteger(x)) {
            throw new IllegalArgumentException("x debe ser entero o medio entero: " + x);
        }
    }

    private static void checkRange(double x) {
        if (x > MAX_ARGUMENT + EPS) {
            throw new IllegalArgumentException("Gamma(" + x + ") excede el rango de double");
        }
    }
}

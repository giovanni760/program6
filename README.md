# Programa 6 - Busqueda de x en la integral de la distribucion t

Encuentra el valor de `x` para el cual integrar la funcion t de 0 a `x` da como
resultado `p`, usando el algoritmo de busqueda del Assignment Kit del Programa 6
(PSP2.1). Reutiliza `Gamma`, `TDistribution` y `SimpsonIntegrator` del Programa 5.

## Clases

| Clase | Funcion |
|---|---|
| `Gamma` | Gamma(x) para enteros y medios enteros |
| `TDistribution` | Funcion de densidad t con `dof` grados de libertad |
| `SimpsonIntegrator` | Integracion numerica con la regla de Simpson |
| `TInverseSearcher` | Algoritmo de busqueda: x = 1.0, d = 0.5, d se divide entre 2 cuando cambia el signo del error |
| `Main` | Imprime la Tabla 1 y permite capturar valores de p y dof |

## Uso

```
mvn -B verify
java -jar target/program6.jar              # Tabla 1 y captura interactiva
java -jar target/program6.jar 0.45 15      # un solo caso
java -jar target/program6.jar --trace 0.45 15   # con la tabla de intentos
```

## Tabla 1

| p | dof | x esperado | x actual |
|---|---|---|---|
| 0.20 | 6 | 0.55338 | 0.55338 |
| 0.45 | 15 | 1.75305 | 1.75305 |
| 0.495 | 4 | 4.60409 | 4.60410 |

El kit sugiere un error aceptable de 0.00001. Con ese valor el tercer caso da
4.60156, porque la curva es casi plana lejos del centro. Por eso el valor por
omision es 0.0000001; el del kit esta disponible como `TInverseSearcher.KIT_ERROR`.

## Jenkins

El `Jenkinsfile` tiene tres etapas: Build (`mvn package`), Test (`mvn verify`, con
reporte JUnit y cobertura JaCoCo) y Deliver (ejecuta el jar).

# Linear Algebra Engine

A concurrent Java engine that evaluates nested matrix expressions. Each operation is split into row-level tasks and run by a pool of workers that prefer the least fatigued thread.

**Authors:** Matan Keysar (212271662), Harel Richter (214431793)

Course project for BGU SPL 2026, Assignment 2.

## What it does

The program reads a JSON expression tree, reduces it to a single matrix, and writes the result to a JSON file. Supported operations:

| Operator | Meaning |
| --- | --- |
| `+` | Element-wise addition. More than two operands are nested left-associatively. |
| `*` | Matrix multiplication. The right-hand matrix is stored column-major. |
| `-` | Unary negation. |
| `T` | Transpose. |

Leaves of the tree are matrices (two-dimensional arrays of numbers). Dimension mismatches and invalid JSON are written to the output file as an error instead of a result matrix.

## How it is built

- **`parser`** — reads the expression tree and writes the result or an error message.
- **`memory`** — `SharedVector` and `SharedMatrix`, guarded by read-write locks. Vectors are either row-major or column-major.
- **`scheduling`** — `TiredThread` workers and a `TiredExecutor`. Each worker has a fatigue factor; idle workers sit in a priority queue ordered by fatigue, so the least tired worker takes the next task. The executor reports time used, idle time, fatigue, and a fairness score (variance of fatigue across workers).
- **`spl.lae`** — `LinearAlgebraEngine` walks the tree, builds one task per row, and waits until that operation finishes before the next node is resolved.

## Requirements

- Java 21
- Maven 3

## Build and run

```bash
mvn test
mvn package
java -jar target/lga-1.0.jar <threads> <input.json> <output.json>
```

`<threads>` is the number of worker threads. Example:

```bash
java -jar target/lga-1.0.jar 4 examples/example1.json out.json
```

On success the output file looks like:

```json
{
  "result" : [ [ -28.0, -31.0 ], [ -106.0, -118.0 ] ]
}
```

The process also prints a per-worker timing report and the fairness score.

## Input shape

An operation node has an `operator` and an `operands` array. An operand is either another operation or a matrix:

```json
{
  "operator": "+",
  "operands": [
    [[1, 2], [3, 4]],
    [[10, 20], [30, 40]]
  ]
}
```

Sample inputs and matching outputs are in `examples/`.

## Tests

```bash
mvn test
```

JUnit 5 covers vectors, matrices, the tired executor and its threads, and end-to-end expression evaluation (addition, multiplication, transpose, negation, and illegal dimensions).

package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        this.executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        try {
            while (true) {
                ComputationNode resolvableNode = computationRoot.findResolvable();
                
                if (resolvableNode == null) {
                    
                    
                    if (computationRoot.getMatrix() != null) { 
                        break; 
                    }

                    
                    Thread.yield();
                    continue; 
                }
                
                if (resolvableNode.getChildren().size() > 2) {
                    resolvableNode.associativeNesting();
                    continue; 
                }
                
                loadAndCompute(resolvableNode);
            }
        } 
        
        catch (Exception e) {
            System.err.println("Computation failed: " + e.getMessage());
            return null;
        }

        finally {
            shutdown();
        }

        return computationRoot;
    }


    public void shutdown() {
        try {
            executor.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Executor shutdown interrupted", e);
        }
    }

    public void loadAndCompute(ComputationNode node) {

        List<ComputationNode> operands = node.getChildren();
        
        if (operands == null || operands.isEmpty()) {
            throw new IllegalArgumentException("Node must have operands to compute.");
        }

        leftMatrix.loadRowMajor(operands.get(0).getMatrix());
        
        if (operands.size() > 1) {
            if (node.getNodeType() == ComputationNodeType.MULTIPLY) {
                rightMatrix.loadColumnMajor(operands.get(1).getMatrix());
            } else {
                rightMatrix.loadRowMajor(operands.get(1).getMatrix());
            }
        }

        List<Runnable> tasks = null;

        switch (node.getNodeType()) {
            case ADD:
                tasks = createAddTasks(); 
                break;
            case MULTIPLY:
                tasks = createMultiplyTasks();
                break;
            case NEGATE:
                tasks = createNegateTasks();
                break;
            case TRANSPOSE:
                tasks = createTransposeTasks();
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation: " + node.getNodeType());
        }

        if (tasks != null && !tasks.isEmpty()) {
            executor.submitAll(tasks);
        }
        
        
        double[][] resultData = leftMatrix.readRowMajor();

        if (node.getNodeType() == ComputationNodeType.TRANSPOSE) {
            resultData = physicalTranspose(resultData);
        }

        node.resolve(resultData);
    }

    public List<Runnable> createAddTasks() { 
        if (leftMatrix.length() != rightMatrix.length()) {
            throw new IllegalArgumentException("Matrix dimension mismatch: Rows count " + 
                                            leftMatrix.length() + " != " + rightMatrix.length());
        }

        int leftWidth = leftMatrix.readRowMajor()[0].length;
        int rightWidth = rightMatrix.readRowMajor()[0].length;
        if (leftWidth != rightWidth) {
            throw new IllegalArgumentException("Matrix dimension mismatch: Columns count " + 
                                            leftWidth + " != " + rightWidth);
        }
        
        List<Runnable> tasks = new ArrayList<>();
        int rowCount = leftMatrix.length();

        for (int i = 0; i < rowCount; i++) {
            final int rowIndex = i;
            tasks.add(() -> {
                SharedVector rowLeft = leftMatrix.get(rowIndex);
                SharedVector rowRight = rightMatrix.get(rowIndex);
                
                rowLeft.add(rowRight); 
            });
        }
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        if (leftMatrix.length() == 0) return new ArrayList<>();

        int colsA = leftMatrix.get(0).length(); 
        int rowsB = rightMatrix.get(0).length(); 
        if (colsA != rowsB) {
            throw new IllegalArgumentException("Matrix multiplication mismatch: Cols A (" + colsA + ") != Rows B (" + rowsB + ")");
        }

        List<Runnable> tasks = new ArrayList<>();
        int rowCount = leftMatrix.length();

        for (int i = 0; i < rowCount; i++) {
            final int rowIndex = i;
            tasks.add(() -> {
                SharedVector row = leftMatrix.get(rowIndex);
                row.vecMatMul(rightMatrix);
            });
        }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        List<Runnable> tasks = new ArrayList<>();
        int rowCount = leftMatrix.length();

        for (int i = 0; i < rowCount; i++) {
            final int rowIndex = i;
            tasks.add(() -> {
                SharedVector row = leftMatrix.get(rowIndex);
                row.negate();
            });
        }
        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        List<Runnable> tasks = new ArrayList<>();
        int rowCount = leftMatrix.length();

        for (int i = 0; i < rowCount; i++) {
            final int rowIndex = i;
            tasks.add(() -> {
                SharedVector row = leftMatrix.get(rowIndex);
                row.transpose();
            });
        }
        return tasks;
    }

    public String getWorkerReport() {
        return executor.getWorkerReport();
    }

    private double[][] physicalTranspose(double[][] matrix) {
        if (matrix == null || matrix.length == 0) { // Handle empty matrix
            return matrix;
        }

        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] transposed = new double[cols][rows]; // Swap dimensions

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed[j][i] = matrix[i][j]; // Swap indices
            }
        }
        return transposed;
    }

    public double getFairness() {
        return executor.getFairness();
    }
}
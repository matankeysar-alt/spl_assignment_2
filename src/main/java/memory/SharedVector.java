package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        this.vector = vector;
        this.orientation = orientation;
    }

    public double get(int index) {
        return vector[index];
    }

    public int length() {
        return vector.length;
    }

    public VectorOrientation getOrientation() {
        return orientation;
    }

    public void writeLock() {
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        lock.writeLock().unlock();
    }

    public void readLock() {
        lock.readLock().lock();
    }

    public void readUnlock() {
        lock.readLock().unlock();
    }

    public void transpose() {
        if (orientation == VectorOrientation.ROW_MAJOR) {
            orientation = VectorOrientation.COLUMN_MAJOR;
        } else {
            orientation = VectorOrientation.ROW_MAJOR;
        }
    }

    public void add(SharedVector other) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < vector.length; i++) {
                vector[i] += other.get(i);
            }
        }
        // to ensure the lock is released even if an exception occurs 
        finally {
            lock.writeLock().unlock();
        }
    }

    public void negate() {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = -vector[i];
            }
        } 
        // to ensure the lock is released even if an exception occurs 
        finally {
            lock.writeLock().unlock();
        }
    }

    public double dot(SharedVector other) {
        lock.readLock().lock();
        try {
            double result = 0;
            for (int i = 0; i < vector.length; i++) {
                result += vector[i] * other.get(i);
            }
            return result;
        } 
        finally {
            lock.readLock().unlock();
        }
    }

    public void vecMatMul(SharedMatrix matrix) {
    lock.writeLock().lock();
    try {
        // --- VALIDATION START ---
        if (this.orientation != VectorOrientation.ROW_MAJOR) {
            throw new IllegalStateException("Vector must be row-major for vecMatMul");
        }
        // CRITICAL: We require the matrix to be Column Major so we can retrieve columns easily
        if (matrix.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
            throw new IllegalArgumentException("Matrix must be column-major for vecMatMul");
        }
        // --- VALIDATION END ---


        // CHANGE 1: Determine the size of the RESULT vector. 
        // A (1x3) vector * (3x2) matrix = (1x2) vector.
        // matrix.length() returns the number of columns (2).
        int resultWidth = matrix.length(); 
        double[] result = new double[resultWidth];

        // CHANGE 2: Iterate over every column of the matrix
        for (int j = 0; j < resultWidth; j++) {
            SharedVector column = matrix.get(j);
            
            // CHANGE 3: The Fix. 
            // We check if the Row Length (3) matches the Column Height (3).
            if (this.length() != column.length()) {
                throw new IllegalArgumentException("Vector length (" + this.length() + 
                                                 ") must match matrix column height (" + column.length() + ")");
            }

            // CHANGE 4: Math.
            // Calculate the dot product: Row * Column[j]
            result[j] = this.dot(column);
        }

        // Apply the result
        this.vector = result;

        } 
        finally {
            lock.writeLock().unlock();
        }
    }
}
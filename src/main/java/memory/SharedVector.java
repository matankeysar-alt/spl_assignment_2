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
        lock.writeLock().lock(); // write lock before modifying the vector
        try {

            if (this.length() != other.length()) {
                throw new IllegalArgumentException("Vector length mismatch: " + this.length() + " != " + other.length());
            }

            for (int i = 0; i < vector.length; i++) {
                vector[i] += other.get(i);
            }
        }
        // to ensure the lock is released even if an exception occurs 
        finally {
            lock.writeLock().unlock(); // release the lock 
        }
    }

    public void negate() {
        lock.writeLock().lock(); // write lock before modifying the vector
        try {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = -vector[i];
            }
        } 
        // to ensure the lock is released even if an exception occurs 
        finally {
            lock.writeLock().unlock(); // release the lock
        }
    }

    // Helper method to calculate dot product (assumes read and write locks are held)
    private double calculateDotProduct(SharedVector other) {

        if (this.length() != other.length()) {
        throw new IllegalArgumentException("Dot product error: vectors must have same length (" 
            + this.length() + " != " + other.length() + ")");
        }

        double result = 0;
        for (int i = 0; i < vector.length; i++) {
            result += vector[i] * other.get(i);
        }

        return result;
    }

    public double dot(SharedVector other) {
        this.lock.readLock().lock(); // acquire read lock on this vector
        other.lock.readLock().lock(); // acquire read lock on other vector
        try {
            return calculateDotProduct(other); 
        } 
        finally {
            //release locks in reverse order
            other.lock.readLock().unlock();
            this.lock.readLock().unlock();
        }
    }

    public void vecMatMul(SharedMatrix matrix) {
    lock.writeLock().lock(); // acquire write lock on this vector
    try {

        if (this.orientation != VectorOrientation.ROW_MAJOR) {
            throw new IllegalStateException("Vector must be row-major for vecMatMul");
        }
    
        if (matrix.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
            throw new IllegalArgumentException("Matrix must be column-major for vecMatMul");
        }

        int matrixWidth = matrix.length(); 
        if (matrixWidth == 0) {
            throw new IllegalArgumentException("Operation failed: Matrix cannot be empty for vecMatMul");
        }

        int matrixHeight = matrix.get(0).length(); // dimensions check
        if (this.length() != matrixHeight) {
            throw new IllegalArgumentException("Vector length (" + this.length() + 
                                               ") must match matrix height (" + matrixHeight + ")");
        }

        double[] result = new double[matrixWidth];

        for (int j = 0; j < matrixWidth; j++) {
            SharedVector column = matrix.get(j);

            column.readLock(); // write lock already held on this vector 
            try {
                result[j] = calculateDotProduct(column);
            } finally {
                column.readUnlock(); // release read lock on column vector
            }
        }

        this.vector = result;

        } 
        finally {
            lock.writeLock().unlock(); // release write lock on this vector
        }
    }
}
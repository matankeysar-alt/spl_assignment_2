import memory.SharedMatrix;
import memory.SharedVector;
import memory.VectorOrientation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

public class matrixTest {

    @Test
    public void testRowMajorLoading() {
        double[][] data = {
            {1.0, 2.0},
            {3.0, 4.0}
        };
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadRowMajor(data);

        assertEquals(2, matrix.length());
        assertEquals(1.0, matrix.get(0).get(0));
        assertEquals(2.0, matrix.get(0).get(1));
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testColumnMajorLoading() {
        double[][] data = {
            {1.0, 2.0},
            {3.0, 4.0}
        };
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(data);

        SharedVector v0 = matrix.get(0);
        SharedVector v1 = matrix.get(1);

        assertEquals(2, matrix.length());
        assertEquals(1.0, v0.get(0));
        assertEquals(3.0, v0.get(1));
        assertEquals(2.0, v1.get(0));
        assertEquals(4.0, v1.get(1));
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testReadRowMajor() {
        double[][] data = {
            {10.0, 20.0},
            {30.0, 40.0}
        };
        SharedMatrix matrix = new SharedMatrix(data);

        double[][] result = matrix.readRowMajor();

        assertTrue(Arrays.deepEquals(data, result));
    }

    @Test
    public void testNonSquareMatrix() {
        double[][] data = {
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0}
        };
        
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(data);
        
        assertEquals(3, matrix.length());
        assertEquals(2, matrix.get(0).length());
        assertEquals(6.0, matrix.get(2).get(1));
    }

    @Test
    public void testEmptyMatrix() {
        SharedMatrix matrix = new SharedMatrix();
        
        assertEquals(0, matrix.length());
        
        matrix.loadRowMajor(new double[][]{});
        assertEquals(0, matrix.length());
        
        double[][] result = matrix.readRowMajor();
        assertEquals(0, result.length);
    }

    @Test
    public void testOrientationLogic() {
        SharedMatrix matrix = new SharedMatrix();
        
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
        
        matrix.loadColumnMajor(new double[][]{{1}});
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
    }
}
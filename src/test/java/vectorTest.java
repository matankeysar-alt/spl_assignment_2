import memory.SharedVector;
import memory.VectorOrientation;
import memory.SharedMatrix;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class vectorTest {

    @Test
    public void testBasicAddition() {
        double[] data1 = {1.0, 2.0, 3.0};
        double[] data2 = {4.0, 5.0, 6.0};

        SharedVector v1 = new SharedVector(data1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(data2, VectorOrientation.ROW_MAJOR);

        v1.add(v2);

        assertEquals(5.0, v1.get(0));
        assertEquals(7.0, v1.get(1));
        assertEquals(9.0, v1.get(2));
    }

    @Test
    public void testDotProduct() {
        double[] data1 = {1.0, 2.0, 3.0};
        double[] data2 = {4.0, 5.0, 6.0};

        SharedVector v1 = new SharedVector(data1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(data2, VectorOrientation.ROW_MAJOR);

        double dotRes = v1.dot(v2);
        
        assertEquals(32.0, dotRes);
    }

    @Test
    public void testNegate() {
        double[] data = {1.0, 2.0, 3.0};
        SharedVector v1 = new SharedVector(data, VectorOrientation.ROW_MAJOR);

        v1.negate();

        assertEquals(-1.0, v1.get(0));
        assertEquals(-2.0, v1.get(1));
        assertEquals(-3.0, v1.get(2));
    }

    @Test
    public void testTransposeToggle() {
        double[] data = {1.0, 2.0, 3.0};
        SharedVector v1 = new SharedVector(data, VectorOrientation.ROW_MAJOR);

        v1.transpose();

        assertEquals(VectorOrientation.COLUMN_MAJOR, v1.getOrientation());
    }

    @Test
    public void testDimensionMismatchDot() {
        SharedVector v1 = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        SharedVector vShort = new SharedVector(new double[]{1.0, 1.0}, VectorOrientation.ROW_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> v1.dot(vShort));
    }

    @Test
    public void testVecMatMul() {
        SharedVector v = new SharedVector(new double[]{1.0, 2.0}, VectorOrientation.ROW_MAJOR);
        double[][] columnsData = {{3.0, 5.0}, {4.0, 6.0}}; 

        SharedMatrix matrix = new SharedMatrix();
        matrix.loadRowMajor(columnsData); 

        for(int i = 0; i < matrix.length(); i++) {
            matrix.get(i).transpose(); 
        }

        assertDoesNotThrow(() -> v.vecMatMul(matrix));
        
        assertEquals(13.0, v.get(0));
        assertEquals(16.0, v.get(1));
    }

    @Test
    public void testVecMatMulDimensionMismatch() {
        SharedVector vTiny = new SharedVector(new double[]{1.0, 2.0}, VectorOrientation.ROW_MAJOR);
        
        SharedMatrix mTall = new SharedMatrix();
        double[][] tallData = {
            {1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0} 
        }; 
        mTall.loadRowMajor(tallData);
        
        for(int i=0; i<mTall.length(); i++) mTall.get(i).transpose();
        
        assertThrows(IllegalArgumentException.class, () -> vTiny.vecMatMul(mTall));
    }

    @Test
    public void testVecMatMulWrongVectorOrientation() {
        SharedVector vCol = new SharedVector(new double[]{1.0, 2.0}, VectorOrientation.COLUMN_MAJOR);
        SharedMatrix mNormal = new SharedMatrix();
        mNormal.loadRowMajor(new double[][]{{1},{1}}); 
        
        assertThrows(IllegalStateException.class, () -> vCol.vecMatMul(mNormal));
    }

    @Test
    public void testVecMatMulWrongMatrixOrientation() {
        SharedVector vGood = new SharedVector(new double[]{1.0, 2.0}, VectorOrientation.ROW_MAJOR);
        SharedMatrix mRowMajor = new SharedMatrix(); 
        mRowMajor.loadRowMajor(new double[][]{{1},{1}}); 

        assertThrows(IllegalArgumentException.class, () -> vGood.vecMatMul(mRowMajor));
    }
}

import spl.lae.LinearAlgebraEngine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import parser.ComputationNode;
import parser.ComputationNodeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LAETest {

    private ComputationNode createNode(ComputationNodeType type, ComputationNode... children) {
        List<ComputationNode> childList = new ArrayList<>(Arrays.asList(children));
        return new ComputationNode(type, childList);
    }

    private ComputationNode createLeaf(double[][] data) {
        return new ComputationNode(data);
    }

    @Test
    public void testSimpleAddition() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        try {
            double[][] dataA = {{1, 2}, {3, 4}};
            double[][] dataB = {{10, 20}, {30, 40}};

            ComputationNode a = createLeaf(dataA);
            ComputationNode b = createLeaf(dataB);
            ComputationNode root = createNode(ComputationNodeType.ADD, a, b);

            ComputationNode result = engine.run(root);

            double[][] expected = {{11, 22}, {33, 44}};

            assertNotNull(result);
            assertTrue(Arrays.deepEquals(result.getMatrix(), expected));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    public void testSimpleMultiplication() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        try {
            double[][] dataA = {{1, 2, 3}, {4, 5, 6}};
            double[][] dataB = {{7, 8}, {9, 1}, {2, 3}};

            ComputationNode a = createLeaf(dataA);
            ComputationNode b = createLeaf(dataB);
            ComputationNode root = createNode(ComputationNodeType.MULTIPLY, a, b);

            ComputationNode result = engine.run(root);

            double[][] expected = {{31.0, 19.0}, {85.0, 55.0}};

            assertNotNull(result);
            assertTrue(Arrays.deepEquals(result.getMatrix(), expected));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    public void testTransposeLogic() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        try {
            double[][] data = {{1, 2, 3}, {4, 5, 6}};
            ComputationNode a = createLeaf(data);
            ComputationNode root = createNode(ComputationNodeType.TRANSPOSE, a);

            ComputationNode result = engine.run(root);

            assertNotNull(result);

            double[][] resMatrix = result.getMatrix();
            double[][] expected = {{1.0, 4.0}, {2.0, 5.0}, {3.0, 6.0}};

            assertEquals(3, resMatrix.length);
            assertEquals(2, resMatrix[0].length);
            assertTrue(Arrays.deepEquals(resMatrix, expected));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    public void testNegateOperation() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        try {
            double[][] data = {{1, -2}, {0, 5}};
            ComputationNode a = createLeaf(data);
            ComputationNode root = createNode(ComputationNodeType.NEGATE, a);

            ComputationNode result = engine.run(root);
            double[][] expected = {{-1.0, 2.0}, {-0.0, -5.0}};

            assertNotNull(result);
            assertTrue(Arrays.deepEquals(result.getMatrix(), expected));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    public void testComplexTreeExecution() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        try {
            double[][] dataA = {{1, 1}, {1, 1}};
            double[][] dataB = {{2, 2}, {2, 2}};
            double[][] dataC = {{2}, {2}};

            ComputationNode a = createLeaf(dataA);
            ComputationNode b = createLeaf(dataB);
            ComputationNode c = createLeaf(dataC);

            ComputationNode addNode = createNode(ComputationNodeType.ADD, a, b);
            ComputationNode root = createNode(ComputationNodeType.MULTIPLY, addNode, c);

            ComputationNode result = engine.run(root);

            double[][] expected = {{12.0}, {12.0}};

            assertNotNull(result);
            assertTrue(Arrays.deepEquals(result.getMatrix(), expected));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    public void testDimensionMismatchError() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        try {
            double[][] dataA = {{1, 1}, {1, 1}};
            double[][] dataB = {{5}};

            ComputationNode a = createLeaf(dataA);
            ComputationNode b = createLeaf(dataB);
            ComputationNode root = createNode(ComputationNodeType.ADD, a, b);

            ComputationNode result = engine.run(root);

            assertNull(result);
        } finally {
            engine.shutdown();
        }
    }
}
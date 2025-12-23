package spl.lae;
import java.io.IOException;
import java.text.ParseException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
      if (args.length < 3) {
            System.err.println("Error: Missing arguments. Usage: java Main <threads> <input> <output>");
            return;
        }
      LinearAlgebraEngine lae = new LinearAlgebraEngine(Integer.parseInt(args[0]));
      InputParser parser = new InputParser();
      ComputationNode root = null;
      try {
        root = parser.parse(args[1]);
        ComputationNode result = lae.run(root);
        if (result != null) {
          OutputWriter.write(result.getMatrix(), args[2]);
        }
        else {
          OutputWriter.write("error: Illegal operation: dimensions mismatch", args[2]);
        }
      } 
      catch (ParseException e) {
        OutputWriter.write(e.getMessage(), args[2]);
      }
      catch (Exception e) {
        OutputWriter.write("error: Illegal operation: dimensions mismatch", args[2]);
      }
      finally {
        lae.shutdown();
      } 
    }
}
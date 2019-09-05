package org.locationtech.jtstest.cmd;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;

import junit.framework.TestCase;

public class JTSOpCmdTest extends TestCase {
  private boolean isVerbose = true;

  public JTSOpCmdTest(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {JTSOpCmdTest.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }
  
  public void testHelp() {
    runCmd( args("-help"), "Usage");
  }
  
  public void testErrorFileNotFoundA() {
    runCmdError( args("-a", "missing.wkt"), 
        JTSOpCmd.ERR_FILE_NOT_FOUND );
  }
  
  public void testErrorFileNotFoundB() {
    runCmdError( args("-b", "missing.wkt"), 
        JTSOpCmd.ERR_FILE_NOT_FOUND );
  }
  
  public void testErrorFunctioNotFound() {
    runCmdError( args("-a", "POINT ( 1 1 )", "buffer" ),
        JTSOpCmd.ERR_FUNCTION_NOT_FOUND );
  }
  
  public void testErrorMissingArgBuffer() {
    runCmdError( args("-a", "POINT ( 1 1 )", "Buffer.buffer" ),
        JTSOpCmd.ERR_WRONG_ARG_COUNT );
  }
  
  public void testErrorMissingGeomABuffer() {
    runCmdError( args("Buffer.buffer", "10" ),
        JTSOpCmd.ERR_REQUIRED_A );
  }
  
  public void testErrorMissingGeomBUnion() {
    runCmdError( args("-a", "POINT ( 1 1 )", "Overlay.union" ),
        JTSOpCmd.ERR_REQUIRED_B );
  }
  
  public void testErrorMissingGeomAUnion() {
    runCmdError( args("-b", "POINT ( 1 1 )", "Overlay.union" ),
        JTSOpCmd.ERR_REQUIRED_A );
  }
  
  public void testOpEnvelope() {
    runCmd( args("-a", "LINESTRING ( 1 1, 2 2)", "-f", "wkt", "envelope"), 
        "POLYGON" );
  }
  
  public void testOpLength() {
    runCmd( args("-a", "LINESTRING ( 1 0, 2 0 )", "-f", "txt", "length"), 
        "1" );
  }
  
  public void testOpUnionLines() {
    runCmd( args("-a", "LINESTRING ( 1 0, 2 0 )", "-b", "LINESTRING ( 2 0, 3 0 )", "-f", "wkt", "Overlay.union"), 
        "MULTILINESTRING ((1 0, 2 0), (2 0, 3 0))" );
  }
  
  public void testFormatWKB() {
    runCmd( args("-a", "LINESTRING ( 1 1, 2 2)", "-f", "wkb"), 
        "0000000002000000023FF00000000000003FF000000000000040000000000000004000000000000000" );
  }
  
  public void testFormatGeoJSON() {
    runCmd( args("-a", "LINESTRING ( 1 1, 2 2)", "-f", "geojson"), 
        "{\"type\":\"LineString\",\"coordinates\":[[1,1],[2,2]]}" );
  }
  
  public void testFormatSVG() {
    runCmd( args("-a", "LINESTRING ( 1 1, 2 2)", "-f", "svg"), 
        "<polyline" );
  }
  
  public void testFormatGML() {
    runCmd( args("-a", "LINESTRING ( 1 1, 2 2)", "-f", "gml"), 
        "<gml:LineString>" );
  }
  
  public void testStdInWKT() {
    runCmd( args("-a", "stdin", "-f", "wkt", "envelope"), 
        stdin("LINESTRING ( 1 1, 2 2)"),
        "POLYGON" );
  }
  
  public void testStdInWKB() {
    runCmd( args("-a", "stdin", "-f", "wkt", "envelope"), 
        stdin("000000000200000005405900000000000040590000000000004072C000000000004062C00000000000405900000000000040690000000000004072C00000000000406F40000000000040590000000000004072C00000000000"),
        "POLYGON" );
  }
  
  public void testStdInBadFormat() {
    runCmdError( args("-a", "stdin", "-f", "wkt", "envelope"), 
        stdin("<gml fdlfld >"),
        JTSOpCmd.ERR_INPUT );
  }
  
  private String[] args(String ... args) {
    return args;
  }

  private static InputStream stdin(String data) {
    InputStream instr = new ByteArrayInputStream(data.getBytes(Charset.forName("UTF-8")));
    return instr;
  }
  private static InputStream stdinFile(String filename) {
    try {
      return new FileInputStream(filename);
    }
    catch (FileNotFoundException ex) {
      throw new RuntimeException("File not found: " + filename);
    }
  }
  
  public void runCmd(String[] args, String expected)
  {    
    runCmd(args, null, expected);
  }

  private void runCmd(String[] args, InputStream stdin, String expected) {
    JTSOpCmd cmd = new JTSOpCmd();
    cmd.captureOutput();
    if (stdin != null) cmd.replaceStdIn(stdin);
    try {
      JTSOpCmd.CmdArgs cmdArgs = cmd.parseArgs(args);
      cmd.execute(cmdArgs);
    } catch (Exception e) {
      e.printStackTrace();
    }
    checkExpected( cmd.getOutput(), expected);
  }
  
  public void runCmdError(String[] args) {
    runCmdError(args, null);
  }

  public void runCmdError(String[] args, String expected) {
    runCmdError(args, null, expected);
  }

  public void runCmdError(String[] args, InputStream stdin, String expected)
  {    
    JTSOpCmd cmd = new JTSOpCmd();
    if (stdin != null) cmd.replaceStdIn(stdin);
    try {
      JTSOpCmd.CmdArgs cmdArgs = cmd.parseArgs(args);
      cmd.execute(cmdArgs);
    } 
    catch (CommandError e) {
      if (expected != null) checkExpected( e.getMessage(), expected );
      return;
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    assertTrue("Expected error but command completed successfully", false);
  }

  private void checkExpected(String actual, String expected) {
    boolean found = actual.contains(expected);
    if (isVerbose  && ! found) {
      System.out.println("Expected: " + expected);
      System.out.println("Actual: " + actual);
    }
    assertTrue( found );
  }
}
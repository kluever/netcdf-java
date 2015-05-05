/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.nc2.util.Indent;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 /*
 message CoordSys {
   required string name = 1;
   repeated string axes = 2;
   repeated CoordTransform transforms = 3;
   repeated CoordSys components = 4;        // ??
 }
  }
 *
 * @author caron
 * @since 5/2/2015
 */
public class GridCoordSys {
  String name;
  List<String> axisNames;
  List<String> transformNames;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getAxisNames() {
    return axisNames;
  }

  public void setAxisNames(List<String> axisNames) {
    this.axisNames = axisNames;
  }

  public void addAxisName(String p) {
    if (axisNames == null) axisNames = new ArrayList<>();
    axisNames.add(p);
  }

  public List<String> getTransformNames() {
    return transformNames;
  }

  public void setTransformNames(List<String> transformNames) {
    this.transformNames = transformNames;
  }

  public void addTransformName(String p) {
    if (transformNames == null) transformNames = new ArrayList<>();
    transformNames.add(p);
  }

  public void toString(Formatter f, Indent indent) {
    indent.incr();
    f.format("%s CoordSys '%s'", indent, name);
    f.format(" has coordVars:");
    for (String v : axisNames)
      f.format("%s, ", v);
    f.format("; has transforms:");
    for (String t : transformNames)
      f.format("%s, ", t);
    f.format("%n");

    indent.decr();
  }
}
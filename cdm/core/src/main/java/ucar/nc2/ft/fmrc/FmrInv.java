/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.fmrc;

import ucar.nc2.time.CalendarDate;
import java.util.*;

/**
 * Inventory for a Forecast Model Run - one runtime.
 * Track inventory by coordinate value, not index.
 * Composed of one or more GridDatasets, each described by a GridDatasetInv.
 * For each Grid, the vert, time and ens coordinates are created as the union of the components.
 * We make sure we are sharing coordinates across grids where they are equivilent.
 * We are thus making a rectangular array var(time, ens, level).
 * So obviously we have to tolerate missing data.
 * <p/>
 * seems to be immutable after finish() is called.
 * 
 * @author caron
 * @since Jan 11, 2010
 */
public class FmrInv implements Comparable<FmrInv> {
  private final List<TimeCoord> timeCoords = new ArrayList<>(); // list of unique TimeCoord
  private final List<EnsCoord> ensCoords = new ArrayList<>(); // list of unique EnsCoord
  private final List<VertCoord> vertCoords = new ArrayList<>(); // list of unique VertCoord
  private final Map<String, GridVariable> uvHash = new HashMap<>(); // hash of FmrInv.Grid
  private List<GridVariable> gridList; // sorted list of FmrInv.Grid

  public List<TimeCoord> getTimeCoords() {
    return timeCoords;
  }

  public List<EnsCoord> getEnsCoords() {
    return ensCoords;
  }

  public List<VertCoord> getVertCoords() {
    return vertCoords;
  }

  public List<GridVariable> getGrids() {
    return gridList;
  }

  public List<GridDatasetInv> getInventoryList() {
    return invList;
  }

  public CalendarDate getRunDate() {
    return runtime;
  }

  public String getName() {
    return "";
  }

  ////////////////////////////////////////////////////////////////////////////////////

  private final List<GridDatasetInv> invList = new ArrayList<>();
  private final CalendarDate runtime;

  FmrInv(CalendarDate runtime) {
    this.runtime = runtime;
  }

  void addDataset(GridDatasetInv inv, Formatter debug) {
    invList.add(inv);

    if (debug != null) {
      debug.format(" Fmr add GridDatasetInv %s = ", inv.getLocation());
      for (TimeCoord tc : inv.getTimeCoords()) {
        debug.format("  %s %n", tc);
      }
    }

    // invert tc -> grid
    for (TimeCoord tc : inv.getTimeCoords()) {
      for (GridDatasetInv.Grid grid : tc.getGridInventory()) {
        GridVariable uv = uvHash.get(grid.getName());
        if (uv == null) {
          uv = new GridVariable(grid.getName());
          uvHash.put(grid.getName(), uv);
        }
        uv.addGridDatasetInv(grid);
      }
    }
  }

  // call after adding all runs
  void finish() {
    gridList = new ArrayList<>(uvHash.values());
    Collections.sort(gridList);

    // find the common coordinates
    for (GridVariable grid : gridList) {
      grid.finish();
    }

    // assign sequence number for time
    int seqno = 0;
    for (TimeCoord tc : timeCoords)
      tc.setId(seqno++);

    // assign sequence number for vertical coords with same name
    HashMap<String, List<VertCoord>> map = new HashMap<>();
    for (VertCoord vc : vertCoords) {
      List<VertCoord> list = map.computeIfAbsent(vc.getName(), k -> new ArrayList<>());
      list.add(vc);
    }
    for (List<VertCoord> list : map.values()) {
      if (!list.isEmpty()) {
        int count = 0;
        for (VertCoord vc : list) {
          if (count > 0)
            vc.setName(vc.getName() + count);
          count++;
        }
      }
    }

  }

  @Override
  public int compareTo(FmrInv fmr) {
    return runtime.compareTo(fmr.getRunDate());
  }

  /**
   * A grid variable for an fmr (one run)
   * A collection of GridDatasetInv.Grid, one for each separate dataset. All have the same runDate.
   * The time and vert coord of the GridVariable is the union of the GridDatasetInv.Grid time and vert coords.
   * 
   * @author caron
   * @since Jan 12, 2010
   */
  public class GridVariable implements Comparable<GridVariable> {
    private final String name;
    private final List<GridDatasetInv.Grid> gridList = new ArrayList<>();
    VertCoord vertCoordUnion; // union of vert coords
    EnsCoord ensCoordUnion; // union of ens coords NOT USED YET
    TimeCoord timeCoordUnion; // union of time coords
    TimeCoord timeExpected; // expected time coords

    GridVariable(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public CalendarDate getRunDate() {
      return FmrInv.this.getRunDate();
    }

    void addGridDatasetInv(GridDatasetInv.Grid grid) {
      gridList.add(grid);
    }

    public List<GridDatasetInv.Grid> getInventory() {
      return gridList;
    }

    public TimeCoord getTimeExpected() {
      return timeExpected;
    }

    public TimeCoord getTimeCoord() {
      return timeCoordUnion;
    }

    public int compareTo(GridVariable o) {
      return name.compareTo(o.name);
    }

    public int getNVerts() {
      return (vertCoordUnion == null) ? 1 : vertCoordUnion.getSize();
    }

    public int countTotal() {
      int total = 0;
      for (GridDatasetInv.Grid grid : gridList)
        total += grid.countTotal();
      return total;
    }

    void finish() {
      if (gridList.size() == 1) {
        GridDatasetInv.Grid grid = gridList.get(0);
        ensCoordUnion = EnsCoord.findEnsCoord(getEnsCoords(), grid.ec);
        vertCoordUnion = VertCoord.findVertCoord(getVertCoords(), grid.vc);
        timeCoordUnion = TimeCoord.findTimeCoord(getTimeCoords(), grid.tc);
        return;
      }

      // run over all ensCoords and construct the union
      List<EnsCoord> ensList = new ArrayList<>();
      EnsCoord ec_union = null;
      for (GridDatasetInv.Grid grid : gridList) {
        EnsCoord ec = grid.ec;
        if (ec == null)
          continue;
        if (ec_union == null)
          ec_union = new EnsCoord(ec);
        else if (!ec_union.equalsData(ec))
          ensList.add(ec);
      }
      if (ec_union != null) {
        if (!ensList.isEmpty())
          EnsCoord.normalize(ec_union, ensList); // add the other coords
        ensCoordUnion = EnsCoord.findEnsCoord(getEnsCoords(), ec_union); // find unique within collection
      }

      // run over all vertCoords and construct the union
      List<VertCoord> vertList = new ArrayList<>();
      VertCoord vc_union = null;
      for (GridDatasetInv.Grid grid : gridList) {
        VertCoord vc = grid.vc;
        if (vc == null)
          continue;
        if (vc_union == null)
          vc_union = new VertCoord(vc);
        else if (!vc_union.equalsData(vc)) {
          vertList.add(vc);
        }
      }
      if (vc_union != null) {
        VertCoord.normalize(vc_union, vertList); // add the other coords
        vertCoordUnion = VertCoord.findVertCoord(getVertCoords(), vc_union); // now find unique within collection
      }

      // run over all timeCoords and construct the union
      List<TimeCoord> timeList = new ArrayList<>();
      for (GridDatasetInv.Grid grid : gridList) {
        TimeCoord tc = grid.tc;
        timeList.add(tc);
      }
      // all time coordinates have the same run date
      TimeCoord tc_union = TimeCoord.makeUnion(timeList, getRunDate()); // add the other coords
      timeCoordUnion = TimeCoord.findTimeCoord(getTimeCoords(), tc_union); // now find unique within collection
    }

  }

  public Set<GridDatasetInv> getFiles() {
    HashSet<GridDatasetInv> fileSet = new HashSet<>();
    for (FmrInv.GridVariable grid : getGrids()) {
      for (GridDatasetInv.Grid inv : grid.getInventory()) {
        fileSet.add(inv.getFile());
      }
    }
    return fileSet;
  }
}

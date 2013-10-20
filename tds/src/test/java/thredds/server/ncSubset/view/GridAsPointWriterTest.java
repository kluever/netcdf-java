package thredds.server.ncSubset.view;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import thredds.mock.params.GridAsPointDataParameters;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.controller.AbstractFeatureDatasetController;
import thredds.server.ncSubset.controller.NcssDiskCache;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.server.ncSubset.view.gridaspoint.PointDataWriter;
import thredds.server.ncSubset.view.gridaspoint.PointDataWriterFactory;
import thredds.server.ncSubset.dataservice.DatasetHandlerAdapter;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.geoloc.LatLonPoint;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class GridAsPointWriterTest {
	
	private PointDataWriter pointDataWriter;
	private SupportedFormat supportedFormat;
	
	private String pathInfo;

	
	private Map<String,List<String>> vars; 
	private GridDataset gridDataset;
	private LatLonPoint point;
	private CalendarDate date; 
	private List<CalendarDate> wDates;
	private CoordinateAxis1DTime tAxis;
	//private DateUnit dateUnit;
	
	@Parameters
	public static List<Object[]> getTestParameters(){
				 		
		return Arrays.asList(new Object[][]{  
				{SupportedFormat.CSV_STREAM, GridAsPointDataParameters.getGroupedVars().get(0) , GridAsPointDataParameters.getPathInfo().get(0), GridAsPointDataParameters.getPoints().get(0) },
				{SupportedFormat.CSV_STREAM, GridAsPointDataParameters.getGroupedVars().get(1) , GridAsPointDataParameters.getPathInfo().get(1), GridAsPointDataParameters.getPoints().get(1) },
				{SupportedFormat.CSV_STREAM, GridAsPointDataParameters.getGroupedVars().get(2) , GridAsPointDataParameters.getPathInfo().get(2), GridAsPointDataParameters.getPoints().get(2) },
				
				{SupportedFormat.XML_STREAM, GridAsPointDataParameters.getGroupedVars().get(0) , GridAsPointDataParameters.getPathInfo().get(0), GridAsPointDataParameters.getPoints().get(0) },
				{SupportedFormat.XML_STREAM, GridAsPointDataParameters.getGroupedVars().get(1) , GridAsPointDataParameters.getPathInfo().get(1), GridAsPointDataParameters.getPoints().get(1) },
				{SupportedFormat.XML_STREAM, GridAsPointDataParameters.getGroupedVars().get(2) , GridAsPointDataParameters.getPathInfo().get(2), GridAsPointDataParameters.getPoints().get(2) },
				
				{SupportedFormat.NETCDF3, GridAsPointDataParameters.getGroupedVars().get(0) , GridAsPointDataParameters.getPathInfo().get(0), GridAsPointDataParameters.getPoints().get(0) },
				{SupportedFormat.NETCDF3, GridAsPointDataParameters.getGroupedVars().get(1) , GridAsPointDataParameters.getPathInfo().get(1), GridAsPointDataParameters.getPoints().get(1) },
				{SupportedFormat.NETCDF3, GridAsPointDataParameters.getGroupedVars().get(2) , GridAsPointDataParameters.getPathInfo().get(2), GridAsPointDataParameters.getPoints().get(2) },
				
				{SupportedFormat.NETCDF4, GridAsPointDataParameters.getGroupedVars().get(0) , GridAsPointDataParameters.getPathInfo().get(0), GridAsPointDataParameters.getPoints().get(0) },
				{SupportedFormat.NETCDF4, GridAsPointDataParameters.getGroupedVars().get(1) , GridAsPointDataParameters.getPathInfo().get(1), GridAsPointDataParameters.getPoints().get(1) },
				{SupportedFormat.NETCDF4, GridAsPointDataParameters.getGroupedVars().get(2) , GridAsPointDataParameters.getPathInfo().get(2), GridAsPointDataParameters.getPoints().get(2) },
		});				
	}
	
	public GridAsPointWriterTest(SupportedFormat supportedFormat, Map<String, List<String>> vars, String pathInfo, LatLonPoint point){
		
		this.supportedFormat = supportedFormat;
		this.vars = vars;		
		this.pathInfo = pathInfo;		
		this.point = point;
	}	
	
	@Before
	public void setUp() throws IOException, OutOfBoundariesException, Exception{
		
    String datasetPath = AbstractFeatureDatasetController.getDatasetPath(this.pathInfo);
		gridDataset = DatasetHandlerAdapter.openGridDataset(datasetPath);
    assert gridDataset != null;

		List<String> keys = new ArrayList<String>( vars.keySet());		
		GridAsPointDataset gridAsPointDataset = NcssRequestUtils.buildGridAsPointDataset(gridDataset, vars.get(keys.get(0)) );		
		
		DiskCache2 diskCache = NcssDiskCache.getInstance().getDiskCache();
    pointDataWriter = PointDataWriterFactory.factory(supportedFormat, new ByteArrayOutputStream(), diskCache);

		List<CalendarDate> dates = gridAsPointDataset.getDates();
		Random rand = new Random();
		int randInt =     rand.nextInt( dates.size());
		int randIntNext = rand.nextInt(dates.size());
		int start = Math.min(randInt, randIntNext);
		int end = Math.max(randInt, randIntNext);
		CalendarDateRange range = CalendarDateRange.of( dates.get(start), dates.get(end));		
		wDates = NcssRequestUtils.wantedDates(gridAsPointDataset, range,0);
		
		date = dates.get(randInt);
		//zAxis = gridDataset.findGridDatatype(vars.get(0)).getCoordinateSystem().getVerticalAxis();
		tAxis = gridDataset.findGridDatatype(vars.get(keys.get(0)).get(0)).getCoordinateSystem().getTimeAxis1D();
		//dateUnit = new DateUnit(tAxis.getUnitsString());
	}
	
	
 
	
	@Test
	public void shouldWriteResponse() throws InvalidRangeException{
		assertTrue( pointDataWriter.header(vars, gridDataset, wDates, tAxis.getAttributes(), point, null));
		assertTrue( pointDataWriter.write(vars, gridDataset, wDates, point, null ));
		assertTrue( pointDataWriter.trailer() );
	}
	
	@After
	public void tearDown(){
	
		gridDataset = null;
		//....
	}

}
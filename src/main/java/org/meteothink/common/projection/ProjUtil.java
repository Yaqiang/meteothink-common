/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.meteothink.common.projection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.InvalidValueException;
import org.locationtech.proj4j.ProjCoordinate;
import org.locationtech.proj4j.parser.Proj4Keyword;
import org.locationtech.proj4j.parser.Proj4Parser;
import org.locationtech.proj4j.proj.Projection;
import org.meteothink.common.PointD;

/**
 *
 * @author wyq
 */
public class ProjUtil {    
    
    /**
     * Create new ProjectionInfo with Proj4 string
     * @param proj4Str proj4 string
     * @return Projection info
     */
    public static CoordinateReferenceSystem factory(String proj4Str) {
        CRSFactory crsf = new CRSFactory();
        proj4Str = proj4Str.replace("+", " +");
        proj4Str = proj4Str.trim();
        return crsf.createFromParameters("custom", proj4Str);
    }
    
    /**
     * Create new ProjectionInfo with ESRI projection string
     * @param esriStr ESRI projection string
     * @return Projection info
     */
    public static CoordinateReferenceSystem factoryESRI(String esriStr) {
        CRSFactory crsFactory = new CRSFactory();
        ProjRegistry registry = new ProjRegistry();
        String[] params = getParameterArray(esriStringToProj4Params(registry, esriStr));
        Proj4Parser parser = new Proj4Parser(crsFactory.getRegistry());
        CoordinateReferenceSystem crs = parser.parse("custom", params);
        return crs;
    }
    
    /**
     * Create new ProjectionInfo with crs
     * @param name ProjectionName
     * @return Projection info
     */
    public static CoordinateReferenceSystem factory(ProjectionName name) {
        CRSFactory crsFactory = new CRSFactory();
        String proj4Str = "+proj=" + name.getProj4Name();        
        return crsFactory.createFromParameters("custom", proj4Str);
    }
    
    private static String[] getParameterArray(Map params) {
        String[] args = new String[params.size()];
        int i = 0;
        Set<String> key = params.keySet();
        for (String s : key) {
            args[i] = "+" + s + "=" + params.get(s);
            i += 1;
        }

        return args;
    }
    
    /**
     * Convert ESRI projection string to Proj4 param map
     * @param registry Registry
     * @param esriString ESRI projection string
     * @return Proj4 param map
     */
    public static Map esriStringToProj4Params(ProjRegistry registry, String esriString) {
        Map params = new HashMap();
        String key, value, name;
        int iStart, iEnd;

        //Projection
        if (!esriString.contains("PROJCS")) {
            key = Proj4Keyword.proj;
            value = "longlat";
            params.put(key, value);
        } else {
            Projection projection = null;
            iStart = esriString.indexOf("PROJECTION") + 12;
            iEnd = esriString.indexOf("]", iStart) - 1;
            String s = esriString.substring(iStart, iEnd);
            if (s != null) {
                projection = registry.getProjectionEsri(s);
                if (projection == null) {
                    throw new InvalidValueException("Unknown projection: " + s);
                }
            }

            String proj4Name = registry.getProj4Name(projection);
            key = Proj4Keyword.proj;
            value = proj4Name;
            params.put(key, value);
        }

        //Datum
        if (esriString.contains("DATUM")) {
            iStart = esriString.indexOf("DATUM") + 7;
            iEnd = esriString.indexOf(",", iStart) - 1;
            if (iEnd > iStart) {
                key = Proj4Keyword.datum;
                value = esriString.substring(iStart, iEnd);
                if (value.equals("D_WGS_1984")) {
                    value = "WGS84";
                } else {
                    value = "WGS84";
                }
                params.put(key, value);
            }
        }

        //Ellipsoid
        if (esriString.contains("SPHEROID")) {
            iStart = esriString.indexOf("SPHEROID") + 9;
            iEnd = esriString.indexOf("]", iStart);
            if (iEnd > iStart) {
                String extracted = esriString.substring(iStart, iEnd);
                String[] terms = extracted.split(",");
                name = terms[0];
                name = name.substring(1, name.length() - 1);
                if (name.equals("WGS_1984")) {
                    name = "WGS84";
                } else {
                    name = "WGS84";
                }
                key = Proj4Keyword.ellps;
                value = name;
                params.put(key, value);
                key = Proj4Keyword.a;
                value = terms[1];
                params.put(key, value);
                key = Proj4Keyword.rf;
                value = terms[2];
                params.put(key, value);
            }
        }

//        //Primem
//        if (esriString.contains("PRIMEM")) {
//            iStart = esriString.indexOf("PRIMEM") + 7;
//            iEnd = esriString.indexOf("]", iStart);
//            if (iEnd > iStart) {
//                String extracted = esriString.substring(iStart, iEnd);
//                String[] terms = extracted.split(",");
//                name = terms[0];
//                name = name.substring(1, name.length() - 1);
//                key = Proj4Keyword.pm;
//                value = terms[1];
//                params.put(key, value);
//            }
//        }

        //Projection parameters
        value = getParameter("False_Easting", esriString);
        if (value != null) {
            key = Proj4Keyword.x_0;
            params.put(key, value);
        }
        value = getParameter("False_Northing", esriString);
        if (value != null) {
            key = Proj4Keyword.y_0;
            params.put(key, value);
        }
        value = getParameter("Central_Meridian", esriString);
        if (value != null) {
            key = Proj4Keyword.lon_0;
            params.put(key, value);
        }
        value = getParameter("Standard_Parallel_1", esriString);
        if (value != null) {
            key = Proj4Keyword.lat_1;
            params.put(key, value);
        }
        value = getParameter("Standard_Parallel_2", esriString);
        if (value != null) {
            key = Proj4Keyword.lat_2;
            params.put(key, value);
        }
        value = getParameter("Scale_Factor", esriString);
        if (value != null) {
            key = Proj4Keyword.k_0;
            params.put(key, value);
        }
        value = getParameter("Latitude_Of_Origin", esriString);
        if (value != null) {
            key = Proj4Keyword.lat_0;
            params.put(key, value);
        }

        //Unit

        return params;
    }
    
    private static String getParameter(String name, String esriString) {
        String result = null;
        String par = "PARAMETER[\"" + name;
        int iStart = esriString.toLowerCase().indexOf(par.toLowerCase());
        if (iStart >= 0) {
            iStart += 13 + name.length();
            int iEnd = esriString.indexOf(",", iStart) - 1;
            result = esriString.substring(iStart, iEnd);
        }
        return result;
    }
    
    /**
     * Reproject a point
     * @param x X
     * @param y Y
     * @param source Source projection info
     * @param dest Destination projection info
     * @return Projected point
     */
    public static PointD reprojectPoint(double x, double y, CoordinateReferenceSystem source, CoordinateReferenceSystem dest) {
        double[][] points = new double[1][];
        points[0] = new double[]{x, y};
        reprojectPoints(points, source, dest, 0, points.length);
        PointD rPoint = new PointD(points[0][0], points[0][1]);
        
        return rPoint;
    }
    
    /**
     * Reproject a point
     * @param point The point
     * @param source Source projection info
     * @param dest Destination projection info
     * @return Projected point
     */
    public static PointD reprojectPoint(PointD point, CoordinateReferenceSystem source, CoordinateReferenceSystem dest) {
        return reprojectPoint(point.X, point.Y, source, dest);
    }
    
    /**
     * Reproject a point
     * @param points The points
     * @param source Source projection info
     * @param dest Destination projection info
     */
    public static void reprojectPoints(double[][] points, CoordinateReferenceSystem source, CoordinateReferenceSystem dest) {
        reprojectPoints(points, source, dest, 0, points.length);
    }
    
    /**
     * Reproject points
     *
     * @param points The points
     * @param source Source projection info
     * @param dest Destination projection info
     * @param startIndex Start index
     * @param numPoints Point number
     */
    public static void reprojectPoints(double[][] points, CoordinateReferenceSystem source, CoordinateReferenceSystem dest, int startIndex, int numPoints) {
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CoordinateTransform trans = ctFactory.createTransform(source, dest);
        if (source.getProjection().toString().equals("LongLat")) {
            for (int i = startIndex; i < startIndex + numPoints; i++) {
                if (i >= points.length) {
                    break;
                }
                if (points[i][0] > 180.0) {
                    points[i][0] -= 360;
                } else if (points[i][0] < -180) {
                    points[i][0] += 360;
                }
            }
        }
        for (int i = startIndex; i < startIndex + numPoints; i++) {
            if (i >= points.length) {
                break;
            }
            ProjCoordinate p1 = new ProjCoordinate(points[i][0], points[i][1]);
            ProjCoordinate p2 = new ProjCoordinate();
            trans.transform(p1, p2);
            points[i][0] = p2.x;
            points[i][1] = p2.y;
        }
    }
    
    /**
     * Calculate scale factor from standard parallel
     *
     * @param stP Standard parallel
     * @return Scale factor
     */
    public static double calScaleFactorFromStandardParallel(double stP) {
        double e = 0.081819191;
        stP = Math.PI * stP / 180;
        double tF;
        if (stP > 0) {
            tF = Math.tan(Math.PI / 4.0 - stP / 2.0) * (Math.pow((1.0 + e * Math.sin(stP)) / (1.0 - e * Math.sin(stP)), e / 2.0));
        } else {
            tF = Math.tan(Math.PI / 4.0 + stP / 2.0) / (Math.pow((1.0 + e * Math.sin(stP)) / (1.0 - e * Math.sin(stP)), e / 2.0));
        }

        double mF = Math.cos(stP) / Math.pow(1.0 - e * e * Math.pow(Math.sin(stP), 2.0), 0.5);
        double k0 = mF * (Math.pow(Math.pow(1.0 + e, 1.0 + e) * Math.pow(1.0 - e, 1.0 - e), 0.5)) / (2.0 * tF);

        return k0;
    }
    
    /**
     * Get is the coordinate reference system or not
     * @param crs Coordinate reference system
     * @return Boolean
     */
    public static boolean isLonLat(CoordinateReferenceSystem crs) {
        return crs.getProjection().getName().equals("longlat");
    }
}

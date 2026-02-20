package za.ac.cput.mapapp;

import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import java.awt.Color;

/**
 * Custom waypoint class for study locations
 */
public class StudyLocationWaypoint extends DefaultWaypoint {
    private final String label;
    private final Color color;
    private final boolean isUserLocation;
    private StudyLocation studyLocation;
    
    public StudyLocationWaypoint(String label, GeoPosition coord, Color color, boolean isUserLocation) {
        super(coord);
        this.label = label;
        this.color = color;
        this.isUserLocation = isUserLocation;
    }
    
    public String getLabel() {
        return label;
    }
    
    public Color getColor() {
        return color;
    }
    
    public boolean isUserLocation() {
        return isUserLocation;
    }
    
    public StudyLocation getStudyLocation() {
        return studyLocation;
    }
    
    public void setStudyLocation(StudyLocation studyLocation) {
        this.studyLocation = studyLocation;
    }
}
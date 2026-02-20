package za.ac.cput.mapapp;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.WaypointPainter;
import java.awt.*;
import java.awt.geom.Point2D;

/**
 *
 * @author abong
 */


/**
 * Custom waypoint painter for study locations
 */
public class StudyLocationWaypointPainter extends WaypointPainter<StudyLocationWaypoint> {
    
    @Override
    protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height) {
        if (getWaypoints() == null) {
            return;
        }
        
        // Enable antialiasing for smoother graphics
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        for (StudyLocationWaypoint waypoint : getWaypoints()) {
            Point2D point = map.getTileFactory().geoToPixel(
                waypoint.getPosition(), map.getZoom());
            
            // Convert to screen coordinates
            Rectangle viewportBounds = map.getViewportBounds();
            int x = (int)(point.getX() - viewportBounds.getX());
            int y = (int)(point.getY() - viewportBounds.getY());
            
            // Only paint if the waypoint is visible
            if (x >= -20 && x <= width + 20 && y >= -20 && y <= height + 20) {
                paintWaypoint(g, waypoint, x, y);
            }
        }
    }
    
    private void paintWaypoint(Graphics2D g, StudyLocationWaypoint waypoint, int x, int y) {
        Color originalColor = g.getColor();
        
        if (waypoint.isUserLocation()) {
            // Paint user location marker (larger, red)
            g.setColor(Color.RED);
            g.fillOval(x - 8, y - 8, 16, 16);
            g.setColor(Color.WHITE);
            g.fillOval(x - 4, y - 4, 8, 8);
            
            // Add label
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            String label = "You";
            int labelWidth = fm.stringWidth(label);
            g.drawString(label, x - labelWidth/2, y - 12);
            
        } else {
            // Paint study location marker (blue)
            g.setColor(Color.BLUE);
            g.fillOval(x - 6, y - 6, 12, 12);
            g.setColor(Color.WHITE);
            g.fillOval(x - 3, y - 3, 6, 6);
            
            // Add member count if available
            if (waypoint.getStudyLocation() != null) {
                g.setColor(Color.BLACK);
                g.setFont(new Font("Arial", Font.PLAIN, 10));
                FontMetrics fm = g.getFontMetrics();
                String memberText = String.valueOf(waypoint.getStudyLocation().getMemberCount());
                int textWidth = fm.stringWidth(memberText);
                g.drawString(memberText, x - textWidth/2, y - 10);
            }
        }
        
        g.setColor(originalColor);
    }
}

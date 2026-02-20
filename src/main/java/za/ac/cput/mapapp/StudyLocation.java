package za.ac.cput.mapapp;

/**
 * Data model class for Study Location
 * @author student
 */
public class StudyLocation {
    private int id;
    private String groupName;
    private String locationName;
    private String building;
    private double latitude;
    private double longitude;
    private int memberCount;
    private double distance; // Distance from user's location
    
    // Constructor
    public StudyLocation(int id, String groupName, String locationName, String building, 
                        double latitude, double longitude, int memberCount) {
        this.id = id;
        this.groupName = groupName;
        this.locationName = locationName;
        this.building = building;
        this.latitude = latitude;
        this.longitude = longitude;
        this.memberCount = memberCount;
        this.distance = 0.0; // Will be calculated later
    }
    
    // Getters
    public int getId() {
        return id;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public String getLocationName() {
        return locationName;
    }
    
    public String getBuilding() {
        return building;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public int getMemberCount() {
        return memberCount;
    }
    
    public double getDistance() {
        return distance;
    }
    
    // Setters
    public void setId(int id) {
        this.id = id;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
    
    public void setBuilding(String building) {
        this.building = building;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }
    
    public void setDistance(double distance) {
        this.distance = distance;
    }
    
    @Override
    public String toString() {
        return String.format("StudyLocation{id=%d, groupName='%s', locationName='%s', building='%s', " +
                           "latitude=%.4f, longitude=%.4f, memberCount=%d, distance=%.2f}", 
                           id, groupName, locationName, building, latitude, longitude, memberCount, distance);
    }
}
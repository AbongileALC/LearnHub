package za.ac.cput.mapapp;

/**
 * Database connection class for Study Locations and Students
 * @author abong
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StudyLocationDBDemo {
    
    // Database connection details for Derby
    private static final String DB_URL = "jdbc:derby://localhost:1527/LocationDB;user=app;password=app";
    
    /**
     * Display all study locations from the database (for testing purposes)
     */
    public void displayAllStudyLocations() {
        Connection con = null;
        Statement stat = null;
        ResultSet rs = null;

        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            String url = "jdbc:derby://localhost:1527/LocationDB;user=app;password=app";
            con = DriverManager.getConnection(url);
            System.out.println("Connected!");

            stat = con.createStatement();
            rs = stat.executeQuery("SELECT * FROM study_locations ORDER BY group_name");
            
            // Display header
            System.out.println("ID | Group Name | Location | Building | Latitude | Longitude | Members");
            System.out.println("================================================================================");
            
            // Display all records
            while (rs.next()) {
                int id = rs.getInt("id");
                String groupName = rs.getString("GROUP_NAME");
                String locationName = rs.getString("LOCATION_TYPE");
                String building = rs.getString("BUILDING_NAME");
                double latitude = rs.getDouble("LATITUDE");
                double longitude = rs.getDouble("LONGITUDE");
                int memberCount = rs.getInt("CAPACITY");
                
                System.out.printf("%d | %s | %s | %s | %.4f | %.4f | %d%n", 
                    id, groupName, locationName, building, latitude, longitude, memberCount);
            }
            
        } catch (Exception e) {
            System.err.println("Error accessing database: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (stat != null) stat.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }
    
    /**
     * Load all study locations from database into a List
     * @return List of StudyLocation objects
     */
    public List<StudyLocation> loadStudyLocations() {
        List<StudyLocation> studyLocations = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            // Load Derby JDBC driver
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            
            // Connect to database
            con = DriverManager.getConnection(DB_URL);
            
            // Prepare SQL query
            String sql = "SELECT id, GROUP_NAME, LOCATION_TYPE, BUILDING_NAME, LATITUDE, LONGITUDE, CAPACITY " +
                        "FROM study_locations ORDER BY group_name";
            stmt = con.prepareStatement(sql);
            
            // Execute query
            rs = stmt.executeQuery();
            
            // Process results
            while (rs.next()) {
                StudyLocation location = new StudyLocation(
                    rs.getInt("id"),
                    rs.getString("GROUP_NAME"),
                    rs.getString("LOCATION_TYPE"),
                    rs.getString("BUILDING_NAME"),
                    rs.getDouble("LATITUDE"),
                    rs.getDouble("LONGITUDE"),
                    rs.getInt("CAPACITY")
                );
                
                studyLocations.add(location);
            }
            
            System.out.println("Successfully loaded " + studyLocations.size() + " study locations from database");
            
        } catch (Exception e) {
            System.err.println("Error loading study locations: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (stmt != null) stmt.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        
        return studyLocations;
    }
    
    public List<StudyLocation> loadStudyLocationsWithDistance(double userLatitude, double userLongitude) {
        List<StudyLocation> studyLocations = loadStudyLocations();
        
        // Calculate distance for each location
        for (StudyLocation location : studyLocations) {
            double distance = calculateDistance(userLatitude, userLongitude, 
                                               location.getLatitude(), location.getLongitude());
            location.setDistance(distance);
        }
        
        return studyLocations;
    }
    
    public Student loadStudentByNumber(String studentNumber) {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Student student = null;
        
        try {
            // Load Derby JDBC driver
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            
            // Connect to database
            con = DriverManager.getConnection(DB_URL);
            
            // Prepare SQL query
            String sql = "SELECT id, first_name, last_name, student_number, email, course " +
                        "FROM students WHERE student_number = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, studentNumber);
            
            // Execute query
            rs = stmt.executeQuery();
            
            // Process result
            if (rs.next()) {
                student = new Student(
                    rs.getInt("id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("student_number"),
                    rs.getString("email"),
                    rs.getString("course")
                );
                
                System.out.println("Successfully loaded student: " + student.getFullName());
            } else {
                System.out.println("Student with number " + studentNumber + " not found in database");
            }
            
        } catch (Exception e) {
            System.err.println("Error loading student: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (stmt != null) stmt.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        
        return student;
    }
    
    public Student loadCurrentStudent() {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Student student = null;
        
        try {
            // Load Derby JDBC driver
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            
            // Connect to database
            con = DriverManager.getConnection(DB_URL);
            
            // Prepare SQL query - get the user (assuming only one record)
            String sql = "SELECT id, first_name, last_name, student_number, email, course " +
                        "FROM students ORDER BY id FETCH FIRST 1 ROW ONLY";
            stmt = con.prepareStatement(sql);
            
            // Execute query
            rs = stmt.executeQuery();
            
            // Process result
            if (rs.next()) {
                student = new Student(
                    rs.getInt("id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("student_number"),
                    rs.getString("email"),
                    rs.getString("course")
                );
                
                System.out.println("Successfully loaded current user: " + student.getFullName());
            } else {
                System.out.println("No user found in database, using default");
                // Return default student if none found
                student = new Student();
            }
            
        } catch (Exception e) {
            System.err.println("Error loading current user: " + e.getMessage());
            e.printStackTrace();
            // Return default student on error
            student = new Student();
        } finally {
            // Clean up resources
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (stmt != null) stmt.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        
        return student;
    }
    
    /**
     * Display all students from the database (for testing purposes)
     */
    public void displayAllStudents() {
        Connection con = null;
        Statement stat = null;
        ResultSet rs = null;

        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            con = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to database!");

            stat = con.createStatement();
            rs = stat.executeQuery("SELECT * FROM students ORDER BY last_name, first_name");
            
            // Display header
            System.out.println("ID | First Name | Last Name | Student Number | Email | Course");
            System.out.println("================================================================================");
            
            // Display all records
            while (rs.next()) {
                int id = rs.getInt("id");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String studentNumber = rs.getString("student_number");
                String email = rs.getString("email");
                String course = rs.getString("course");
                
                System.out.printf("%d | %s | %s | %s | %s | %s%n", 
                    id, firstName, lastName, studentNumber, email, course);
            }
            
        } catch (Exception e) {
            System.err.println("Error accessing students table: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (stat != null) stat.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // Distance in km
        
        return distance;
    }
    
    /**
     * Test database connection
     * @return true if connection successful, false otherwise
     */
    public boolean testConnection() {
        Connection con = null;
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            con = DriverManager.getConnection(DB_URL);
            System.out.println("Database connection test successful!");
            return true;
        } catch (Exception e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }
    
     /**
     * Load the user's saved location (latitude and longitude) from the database.
     * @return double[] { latitude, longitude } or null if not found
     */
    public double[] loadUserLocation() {
        double[] location = null;
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            con = DriverManager.getConnection(DB_URL);

            // Create table if it doesn’t exist
            createUserLocationTableIfNotExists(con);

            String sql = "SELECT latitude, longitude FROM user_location WHERE id = 1";
            stmt = con.prepareStatement(sql);
            rs = stmt.executeQuery();

            if (rs.next()) {
                location = new double[]{
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")
                };
                System.out.println("Loaded user location: " + location[0] + ", " + location[1]);
            } else {
                System.out.println("No saved user location found.");
            }

        } catch (Exception e) {
            System.err.println("Error loading user location: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (stmt != null) stmt.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }

        return location;
    }

    public boolean saveUserLocation(double latitude, double longitude) {
        Connection con = null;
        PreparedStatement stmt = null;
        Statement cleanup = null;

        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            con = DriverManager.getConnection(DB_URL);

            // Create table if it doesn’t exist
            createUserLocationTableIfNotExists(con);

            // Delete existing record
            cleanup = con.createStatement();
            cleanup.executeUpdate("DELETE FROM user_location WHERE id = 1");

            // Insert new record
            String sql = "INSERT INTO user_location (id, latitude, longitude) VALUES (1, ?, ?)";
            stmt = con.prepareStatement(sql);
            stmt.setDouble(1, latitude);
            stmt.setDouble(2, longitude);

            int rows = stmt.executeUpdate();
            System.out.println("Saved user location: " + latitude + ", " + longitude);
            return rows > 0;

        } catch (Exception e) {
            System.err.println("Error saving user location: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try { if (stmt != null) stmt.close(); } catch (Exception e) {}
            try { if (cleanup != null) cleanup.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }

    /**
     * Create the user_location table if it does not exist.
     */
    private void createUserLocationTableIfNotExists(Connection con) {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            String createTableSQL =
                    "CREATE TABLE user_location (" +
                    "id INT PRIMARY KEY, " +
                    "latitude DOUBLE NOT NULL, " +
                    "longitude DOUBLE NOT NULL, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.executeUpdate(createTableSQL);
            System.out.println("Created user_location table successfully.");
        } catch (SQLException e) {
            // Ignore "table already exists" errors
            if (!"X0Y32".equals(e.getSQLState())) {
                e.printStackTrace();
            }
        } finally {
            try { if (stmt != null) stmt.close(); } catch (Exception e) {}
        }
    }

}
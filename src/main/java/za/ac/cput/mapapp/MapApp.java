package za.ac.cput.mapapp;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MapApp - main JFrame for Study Groups Location Search
 * @abong
 */
public class MapApp extends JFrame implements ActionListener {
    private final Color LIGHT_BLUE = new Color(173, 216, 230);
    private final Color BUTTON_BLUE = new Color(135, 206, 235);
    private final Color SELECTED_BLUE = new Color(70, 130, 180);
    private final Color GREY_BACKGROUND = new Color(240, 240, 240);

    private boolean isLocationSelectionMode = false;
    private JButton setLocationButton;
    private JPanel locationModePanel;

    private JPanel mapPanel;
    private JScrollPane groupsScrollPane;
    private boolean mapExpanded = false;
    private final int normalMapHeight = 200;
    private final int expandedMapHeight = 350;
    private final int normalFrameHeight = 680;
    private final int expandedFrameHeight = 830;
    private boolean isGridView = false;
    private JButton listBtn, gridBtn;
    private JComboBox<String> sortCombo;
    private JButton expandButton;

    // JXMapViewer components
    private JXMapViewer mapViewer;
    private WaypointPainter<StudyLocationWaypoint> waypointPainter;
    private Set<StudyLocationWaypoint> waypoints;

    // Movement control variables
    private boolean isDragging = false;
    private Point lastMousePoint;

    // User's current location (default)
    private double userLatitude = -33.93080102488844;
    private double userLongitude = 18.430230425585137;
    private GeoPosition userPosition;

    // List to store study locations from database
    private List<StudyLocation> studyLocations;

    // Database connection helper
    private StudyLocationDBDemo dbHelper;

    // Current student information
    private Student currentStudent;

    public MapApp() {
        setTitle("Study Groups Location Search");
        setSize(890, normalFrameHeight);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);
        setBackground(Color.WHITE);
        setResizable(false);

        // Initialize DB helper and load data
        dbHelper = new StudyLocationDBDemo();

        // Load user location from DB (or default)
        loadUserLocationFromDatabase();
        userPosition = new GeoPosition(userLatitude, userLongitude);

        // Load student info and study locations
        loadStudentData();
        initializeDatabase();

        // Network diagnostics
        System.out.println("=== NETWORK DIAGNOSTICS ===");
        configureNetworkSettings();
        checkSecurityPolicy();
        testFirewallConnectivity();
        testNetworkConnectivity();
        System.out.println("=== END DIAGNOSTICS ===");

        // Build UI
        createHeader();
        createNavigationButtons();
        createMapSection();
        createGroupsSection();
        createTrademark();

        // Setup map interactions
        setupMouseControls();

        setVisible(true);
    }

    // ---------------- Network diagnostics ----------------

    private void testNetworkConnectivity() {
        try {
            java.net.URL url = new java.net.URL("https://tile.openstreetmap.org/1/0/0.png");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            System.out.println("Network test response code: " + responseCode);

            if (responseCode == 200) {
                System.out.println("Network connectivity OK");
            } else {
                System.out.println("Network issue detected - Response: " + responseCode);
            }

        } catch (Exception e) {
            System.err.println("Network connectivity failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configureNetworkSettings() {
        // Use system proxies if present
        System.setProperty("java.net.useSystemProxies", "true");

        System.out.println("HTTP Proxy Host: " + System.getProperty("http.proxyHost"));
        System.out.println("HTTP Proxy Port: " + System.getProperty("http.proxyPort"));
        System.out.println("HTTPS Proxy Host: " + System.getProperty("https.proxyHost"));
        System.out.println("HTTPS Proxy Port: " + System.getProperty("https.proxyPort"));

        // set agent to avoid some servers rejecting default java user agents
        System.setProperty("http.agent", "LearnHub/1.0 Java");
    }

    private void checkSecurityPolicy() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            System.out.println("Security manager is active - this may block network requests");
            try {
                security.checkConnect("tile.openstreetmap.org", 443);
                System.out.println("HTTPS connection to OSM allowed");
            } catch (SecurityException e) {
                System.err.println("Security policy blocks OSM connection: " + e.getMessage());
            }
        } else {
            System.out.println("No security manager detected");
        }
    }

    private void testFirewallConnectivity() {
        String[] testUrls = {
                "https://tile.openstreetmap.org",
                "https://www.google.com",
                "https://httpbin.org/get"
        };

        for (String testUrl : testUrls) {
            try {
                java.net.URL url = new java.net.URL(testUrl);
                java.net.URLConnection connection = url.openConnection();
                connection.setConnectTimeout(3000);
                connection.connect();
                System.out.println("✓ Can connect to: " + testUrl);
            } catch (Exception e) {
                System.out.println("✗ Cannot connect to: " + testUrl + " - " + e.getMessage());
            }
        }
    }

    // ---------------- Load / DB helpers ----------------

    private void loadStudentData() {
        try {
            if (dbHelper.testConnection()) {
                currentStudent = dbHelper.loadCurrentStudent();
                if (currentStudent == null) {
                    System.out.println("No student data found, using default");
                    currentStudent = new Student();
                }
            } else {
                System.err.println("Database connection failed, using default student data");
                currentStudent = new Student();
            }
        } catch (Exception e) {
            System.err.println("Error loading student data: " + e.getMessage());
            currentStudent = new Student();
        }

        System.out.println("Loaded student: " + currentStudent.getFullName());
    }

    private void initializeDatabase() {
        studyLocations = new ArrayList<>();

        try {
            if (dbHelper.testConnection()) {
                studyLocations = dbHelper.loadStudyLocationsWithDistance(userLatitude, userLongitude);

                for (StudyLocation location : studyLocations) {
                    location.setDistance(calculateDistance(userLatitude, userLongitude,
                            location.getLatitude(), location.getLongitude()));
                }

                System.out.println("Loaded " + studyLocations.size() + " study locations from Derby database");
            } else {
                throw new Exception("Database connection failed");
            }

        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
            loadSampleData();
        }
    }

    private void loadSampleData() {
        studyLocations.add(new StudyLocation(1, "ADF2625 GROUP", "Library", "Library Building",
                -33.930505201808685, 18.430816189682822, 5));

        studyLocations.add(new StudyLocation(2, "MAF Group", "Library", "Library Building",
                -33.93009832484135, 18.430670728029888, 4));

        studyLocations.add(new StudyLocation(3, "PRJ152S", "Engineering Lab", "Engineering Building",
                -33.930877181287876, 18.42936060636949, 6));

        studyLocations.add(new StudyLocation(4, "Business Practice", "Commerce Building", "Commerce Building",
                -33.930204714751454, 18.42946654578125, 4));

        studyLocations.add(new StudyLocation(5, "Proff Com", "E-Learning Center", "E-Learning Building",
                -33.92893680677932, 18.42840885211103, 3));

        for (StudyLocation location : studyLocations) {
            location.setDistance(calculateDistance(userLatitude, userLongitude,
                    location.getLatitude(), location.getLongitude()));
        }

        System.out.println("Loaded sample data with updated coordinates");
    }

    /**
     * Loads user location from database using dbHelper (keeps single method)
     */
    private void loadUserLocationFromDatabase() {
        try {
            double[] location = dbHelper.loadUserLocation();

            if (location != null && location.length == 2) {
                userLatitude = location[0];
                userLongitude = location[1];
                userPosition = new GeoPosition(userLatitude, userLongitude);
                System.out.println("Loaded user location from DB: " + userLatitude + ", " + userLongitude);
            } else {
                // Use default Cape Town coordinates
                userLatitude = -33.93080102488844;
                userLongitude = 18.430230425585137;
                userPosition = new GeoPosition(userLatitude, userLongitude);
                System.out.println("No saved location, using default");
            }
        } catch (Exception e) {
            System.err.println("Error reading user location from DB: " + e.getMessage());
            userLatitude = -33.93080102488844;
            userLongitude = 18.430230425585137;
            userPosition = new GeoPosition(userLatitude, userLongitude);
        }
    }

    // ---------------- UI: Header & Navigation ----------------

    private void createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBounds(0, 0, 870, 70);
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Left section: User icon and details
        JPanel userDetailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        userDetailsPanel.setBackground(Color.WHITE);

        // Dynamic User Icon with first letter of name
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(LIGHT_BLUE);
                g2d.fillOval(0, 0, getWidth(), getHeight());
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 18));

                FontMetrics fm = g2d.getFontMetrics();
                String text = String.valueOf(currentStudent.getFirstInitial());
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(text, x, y);
            }
        };
        iconPanel.setPreferredSize(new Dimension(40, 40));
        iconPanel.setOpaque(false);

        // User text info panel with dynamic data
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Color.WHITE);

        JLabel nameLabel = new JLabel(currentStudent.getFullName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel studentNoLabel = new JLabel(currentStudent.getStudentNumber());
        studentNoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        studentNoLabel.setForeground(Color.GRAY);
        studentNoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel emailLabel = new JLabel(currentStudent.getEmail());
        emailLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        emailLabel.setForeground(Color.GRAY);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(nameLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(studentNoLabel);
        textPanel.add(emailLabel);

        userDetailsPanel.add(iconPanel);
        userDetailsPanel.add(textPanel);

        // Right section: Logo
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        logoPanel.setBackground(Color.WHITE);

        JLabel logoLabel;
        try {
            java.net.URL logoUrl = getClass().getResource("/learnHub_Logo.png");
            if (logoUrl != null) {
                ImageIcon originalIcon = new ImageIcon(logoUrl);
                Image scaledImage = originalIcon.getImage().getScaledInstance(150, 50, Image.SCALE_SMOOTH);
                logoLabel = new JLabel(new ImageIcon(scaledImage));
            } else {
                logoLabel = new JLabel("LEARN HUB");
                logoLabel.setFont(new Font("Arial", Font.BOLD, 14));
                logoLabel.setForeground(SELECTED_BLUE);
            }
        } catch (Exception e) {
            logoLabel = new JLabel("LEARN HUB");
            logoLabel.setFont(new Font("Arial", Font.BOLD, 14));
            logoLabel.setForeground(SELECTED_BLUE);
        }

        logoPanel.add(logoLabel);

        headerPanel.add(userDetailsPanel, BorderLayout.WEST);
        headerPanel.add(logoPanel, BorderLayout.EAST);

        add(headerPanel);
    }

    private void createNavigationButtons() {
        JPanel navPanel = new JPanel(new GridLayout(1, 6, 5, 0));
        navPanel.setBounds(15, 70, 840, 45);
        navPanel.setBackground(Color.WHITE);
        navPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        String[] buttonTexts = {"DASHBOARD", "PROFILE", "GROUPS", "LOCATION", "MESSAGES", "STUDY POINTS"};
        String[] actionCommands = {"NAV_DASHBOARD", "NAV_PROFILE", "NAV_GROUPS", "NAV_LOCATION", "NAV_MESSAGES", "NAV_STUDY_POINTS"};

        for (int i = 0; i < buttonTexts.length; i++) {
            String text = buttonTexts[i];
            String command = actionCommands[i];

            JButton button = new JButton(text);
            button.setFont(new Font("Arial", Font.PLAIN, 11));
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            button.setFocusPainted(false);
            button.setHorizontalAlignment(SwingUtilities.CENTER);
            button.setActionCommand(command);
            button.addActionListener(this);

            if (text.equals("LOCATION")) {
                button.setForeground(SELECTED_BLUE);
                button.setFont(new Font("Arial", Font.BOLD, 11));
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(5, 10, 3, 10),
                        BorderFactory.createMatteBorder(0, 0, 2, 0, SELECTED_BLUE)
                ));
            } else {
                button.setForeground(Color.GRAY);
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (!button.getForeground().equals(SELECTED_BLUE)) {
                            button.setForeground(Color.BLACK);
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (!button.getForeground().equals(SELECTED_BLUE)) {
                            button.setForeground(Color.GRAY);
                        }
                    }
                });
            }

            navPanel.add(button);
        }

        add(navPanel);
    }

    // ---------------- Map & Waypoints UI ----------------

    private void createMapSection() {
        JPanel mapTitlePanel = new JPanel(new BorderLayout());
        mapTitlePanel.setBounds(20, 125, 830, 30);
        mapTitlePanel.setBackground(Color.LIGHT_GRAY);
        mapTitlePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JLabel mapTitle = new JLabel("STUDY GROUPS MAP");
        mapTitle.setFont(new Font("Arial", Font.BOLD, 12));
        mapTitle.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        expandButton = new JButton("EXPAND");
        expandButton.setFont(new Font("Arial", Font.PLAIN, 10));
        expandButton.setBackground(BUTTON_BLUE);
        expandButton.setForeground(Color.BLACK);
        expandButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        expandButton.setFocusPainted(false);
        expandButton.addActionListener(this);
        expandButton.setActionCommand("EXPAND");

        // "Set My Location" button
        setLocationButton = new JButton("📍 SET MY LOCATION");
        setLocationButton.setFont(new Font("Arial", Font.BOLD, 10));
        setLocationButton.setBackground(new Color(255, 165, 0)); // Orange
        setLocationButton.setForeground(Color.WHITE);
        setLocationButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        setLocationButton.setFocusPainted(false);
        setLocationButton.addActionListener(e -> toggleLocationSelectionMode());

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        rightControls.setBackground(Color.LIGHT_GRAY);
        rightControls.add(setLocationButton);
        rightControls.add(expandButton);

        mapTitlePanel.add(mapTitle, BorderLayout.WEST);
        mapTitlePanel.add(rightControls, BorderLayout.EAST);

        add(mapTitlePanel);

        // Create location mode instruction panel (initially hidden)
        createLocationModePanel();

        mapPanel = new JPanel(new BorderLayout());
        mapPanel.setBounds(20, 155, 830, normalMapHeight);
        mapPanel.setBackground(Color.WHITE);
        mapPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        mapViewer = new JXMapViewer();
        TileFactoryInfo info = new TileFactoryInfo(
                1, 15, 17,
                256, true, true,
                "https://tile.openstreetmap.org",
                "x", "y", "z") {
            @Override
            public String getTileUrl(int x, int y, int zoom) {
                int z = 17 - zoom;
                return String.format("https://tile.openstreetmap.org/%d/%d/%d.png", z, x, y);
            }
        };
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        System.setProperty("http.agent", "LearnHub/1.0 Java");
        mapViewer.setTileFactory(tileFactory);

        mapViewer.setZoom(8);
        mapViewer.setAddressLocation(userPosition);

        waypointPainter = new StudyLocationWaypointPainter();
        waypoints = new HashSet<>();

        setupMapWaypoints();

        List<Painter<JXMapViewer>> painters = new ArrayList<>();
        painters.add(waypointPainter);

        CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(painters);
        mapViewer.setOverlayPainter(painter);

        mapPanel.add(mapViewer, BorderLayout.CENTER);
        add(mapPanel);
    }

    private void createLocationModePanel() {
        locationModePanel = new JPanel(new BorderLayout());
        locationModePanel.setBounds(20, 155, 830, 40);
        locationModePanel.setBackground(new Color(255, 255, 200)); // Light yellow
        locationModePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 165, 0), 2),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        locationModePanel.setVisible(false);

        JLabel instructionLabel = new JLabel("📍 LOCATION SELECTION MODE: Click on the map to set your location");
        instructionLabel.setFont(new Font("Arial", Font.BOLD, 12));
        instructionLabel.setForeground(new Color(139, 69, 19));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(new Color(255, 255, 200));

        JButton confirmBtn = new JButton("✓ CONFIRM");
        confirmBtn.setFont(new Font("Arial", Font.BOLD, 10));
        confirmBtn.setBackground(new Color(34, 139, 34));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        confirmBtn.setFocusPainted(false);
        confirmBtn.addActionListener(e -> confirmLocationSelection());

        JButton cancelBtn = new JButton("✗ CANCEL");
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 10));
        cancelBtn.setBackground(new Color(220, 20, 60));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> cancelLocationSelection());

        buttonPanel.add(confirmBtn);
        buttonPanel.add(cancelBtn);

        locationModePanel.add(instructionLabel, BorderLayout.WEST);
        locationModePanel.add(buttonPanel, BorderLayout.EAST);

        add(locationModePanel);
    }

    private void toggleLocationSelectionMode() {
        isLocationSelectionMode = true;
        locationModePanel.setVisible(true);
        setLocationButton.setEnabled(false);

        // Adjust map panel position
        mapPanel.setBounds(20, 195, 830, normalMapHeight);

        // Adjust all components below the map
        adjustComponentsVertically(40);

        // Change cursor
        mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        // Show instruction
        JOptionPane.showMessageDialog(
                this,
                "Click anywhere on the map to set your new location.\n" +
                        "Use right-click to pan and scroll to zoom.\n" +
                        "Click CONFIRM when you're done, or CANCEL to abort.",
                "Location Selection Mode",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void confirmLocationSelection() {
        if (isLocationSelectionMode) {
            // Save the current user position to database using dbHelper
            if (dbHelper.saveUserLocation(userPosition.getLatitude(), userPosition.getLongitude())) {
                JOptionPane.showMessageDialog(
                        this,
                        String.format("Location saved successfully!\nLat: %.6f, Lon: %.6f",
                                userPosition.getLatitude(),
                                userPosition.getLongitude()),
                        "Location Confirmed",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // Recalculate distances for all study locations
                for (StudyLocation location : studyLocations) {
                    location.setDistance(calculateDistance(userLatitude, userLongitude,
                            location.getLatitude(), location.getLongitude()));
                }

                // Refresh the UI
                updateGroupsList();
                setupMapWaypoints();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to save location to database.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }

            exitLocationSelectionMode();
        }
    }

    private void cancelLocationSelection() {
        // Reload original location from database
        loadUserLocationFromDatabase();
        exitLocationSelectionMode();

        JOptionPane.showMessageDialog(
                this,
                "Location selection cancelled. Your original location is preserved.",
                "Cancelled",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void exitLocationSelectionMode() {
        isLocationSelectionMode = false;
        locationModePanel.setVisible(false);
        setLocationButton.setEnabled(true);

        // Reset map panel position
        mapPanel.setBounds(20, 155, 830, normalMapHeight);

        // Adjust all components back
        adjustComponentsVertically(-40);

        // Reset cursor
        mapViewer.setCursor(Cursor.getDefaultCursor());

        revalidate();
        repaint();
    }

    private void adjustComponentsVertically(int delta) {
        Component[] components = getContentPane().getComponents();
        for (Component comp : components) {
            // Skip the location mode panel and the map panel itself
            if (comp != locationModePanel && comp != mapPanel && comp.getBounds().y >= 375) {
                Rectangle bounds = comp.getBounds();
                bounds.y += delta;
                comp.setBounds(bounds);
            }
        }
        revalidate();
        repaint();
    }

    // ---------------- Map interactions (mouse/zoom/panning) ----------------

    private void setupMouseControls() {
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isDragging = true;
                    lastMousePoint = e.getPoint();
                    mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    centerMapOnClick(e.getPoint());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isDragging = false;
                    mapViewer.setCursor(Cursor.getDefaultCursor());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    zoomIn();
                    centerMapOnClick(e.getPoint());
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    checkWaypointClick(e);
                }
            }
        });

        mapViewer.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging && lastMousePoint != null) {
                    handleMousePanning(e.getPoint());
                }
            }
        });

        mapViewer.addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) {
                zoomIn();
            } else {
                zoomOut();
            }
        });
    }

    private void handleMousePanning(Point currentPoint) {
        if (lastMousePoint == null) return;

        int deltaX = currentPoint.x - lastMousePoint.x;
        int deltaY = currentPoint.y - lastMousePoint.y;

        GeoPosition currentCenter = mapViewer.getCenterPosition();
        Point2D centerPixel = mapViewer.getTileFactory().geoToPixel(currentCenter, mapViewer.getZoom());

        Point2D newCenterPixel = new Point2D.Double(
                centerPixel.getX() - deltaX,
                centerPixel.getY() - deltaY
        );

        GeoPosition newCenter = mapViewer.getTileFactory().pixelToGeo(newCenterPixel, mapViewer.getZoom());
        mapViewer.setCenterPosition(newCenter);
        lastMousePoint = currentPoint;
    }

    private void centerMapOnClick(Point clickPoint) {
        GeoPosition clickedPosition = mapViewer.convertPointToGeoPosition(clickPoint);
        mapViewer.setCenterPosition(clickedPosition);
    }

    private void checkWaypointClick(MouseEvent e) {
        // If in location selection mode, set user location
        if (isLocationSelectionMode) {
            GeoPosition clickedPosition = mapViewer.convertPointToGeoPosition(e.getPoint());

            // Update user position
            userLatitude = clickedPosition.getLatitude();
            userLongitude = clickedPosition.getLongitude();
            userPosition = clickedPosition;

            // Update waypoints to show new user location
            setupMapWaypoints();
            mapViewer.repaint();

            // Update the instruction label (if present)
            Component[] components = locationModePanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JLabel) {
                    ((JLabel) comp).setText("📍 Location: " + String.format("%.6f, %.6f", userLatitude, userLongitude) + " - Click CONFIRM to save");
                    break;
                }
            }

            return;
        }

        // Original waypoint click logic for study locations
        Point2D point = e.getPoint();
        for (StudyLocationWaypoint waypoint : waypoints) {
            if (waypoint.getStudyLocation() != null) {
                Point2D waypointPoint = mapViewer.convertGeoPositionToPoint(waypoint.getPosition());
                double distance = point.distance(waypointPoint);

                if (distance < 20) {
                    showWaypointInfo(waypoint.getStudyLocation());
                    break;
                }
            }
        }
    }

    private void zoomIn() {
        int currentZoom = mapViewer.getZoom();
        if (currentZoom > 1) {
            mapViewer.setZoom(currentZoom - 1);
        }
    }

    private void zoomOut() {
        int currentZoom = mapViewer.getZoom();
        if (currentZoom < 15) {
            mapViewer.setZoom(currentZoom + 1);
        }
    }

    private void centerOnUser() {
        mapViewer.setAddressLocation(userPosition);
        mapViewer.setZoom(8);
    }

    private void centerOnLocation(StudyLocation location) {
        GeoPosition position = new GeoPosition(location.getLatitude(), location.getLongitude());
        mapViewer.setAddressLocation(position);
        mapViewer.setZoom(7);
    }

    // ---------------- Waypoints ----------------

    private void setupMapWaypoints() {
        waypoints = new HashSet<>();

        StudyLocationWaypoint userWaypoint = new StudyLocationWaypoint(
                "Your Location", userPosition, Color.RED, true
        );
        waypoints.add(userWaypoint);

        for (StudyLocation location : studyLocations) {
            GeoPosition position = new GeoPosition(location.getLatitude(), location.getLongitude());
            StudyLocationWaypoint waypoint = new StudyLocationWaypoint(
                    location.getGroupName(), position, Color.BLUE, false
            );
            waypoint.setStudyLocation(location);
            waypoints.add(waypoint);
        }

        waypointPainter.setWaypoints(waypoints);
        mapViewer.repaint();
    }

    private void updateMapWaypoints() {
        waypoints.clear();

        StudyLocationWaypoint userWaypoint = new StudyLocationWaypoint(
                "Your Location", userPosition, Color.RED, true
        );
        waypoints.add(userWaypoint);

        List<StudyLocation> filteredLocations = getFilteredAndSortedLocations();
        for (StudyLocation location : filteredLocations) {
            GeoPosition position = new GeoPosition(location.getLatitude(), location.getLongitude());
            StudyLocationWaypoint waypoint = new StudyLocationWaypoint(
                    location.getGroupName(), position, Color.BLUE, false
            );
            waypoint.setStudyLocation(location);
            waypoints.add(waypoint);
        }

        waypointPainter.setWaypoints(waypoints);
        mapViewer.repaint();
    }

    private void showWaypointInfo(StudyLocation location) {
        String info = String.format(
                "Group: %s\nLocation: %s\nBuilding: %s\nMembers: %d\nDistance: %s",
                location.getGroupName(),
                location.getLocationName(),
                location.getBuilding(),
                location.getMemberCount(),
                formatDistance(location.getDistance())
        );

        JOptionPane.showMessageDialog(this, info, "Study Group Information", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------------- Groups / List / Grid UI ----------------

    private void createGroupsSection() {
        JLabel groupsLabel = new JLabel("GROUPS NEARBY");
        groupsLabel.setBounds(20, 375, 150, 20);
        groupsLabel.setFont(new Font("Arial", Font.BOLD, 12));
        add(groupsLabel);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlsPanel.setBounds(500, 375, 350, 25);
        controlsPanel.setBackground(new Color(239, 239, 239));

        listBtn = new JButton("LIST");
        listBtn.setPreferredSize(new Dimension(60, 25));
        listBtn.setBackground(BUTTON_BLUE);
        listBtn.setForeground(Color.WHITE);
        listBtn.setFont(new Font("Arial", Font.PLAIN, 10));
        listBtn.setBorder(BorderFactory.createEmptyBorder());
        listBtn.setFocusPainted(false);
        listBtn.addActionListener(this);
        listBtn.setActionCommand("LIST");

        gridBtn = new JButton("GRID");
        gridBtn.setPreferredSize(new Dimension(60, 25));
        gridBtn.setBackground(Color.WHITE);
        gridBtn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        gridBtn.setFont(new Font("Arial", Font.PLAIN, 10));
        gridBtn.setFocusPainted(false);
        gridBtn.addActionListener(this);
        gridBtn.setActionCommand("GRID");

        // Sort dropdown
        sortCombo = new JComboBox<>();
        sortCombo.addItem("SORT BY DISTANCE");
        sortCombo.addItem("SORT BY NAME");
        sortCombo.addItem("SORT BY MEMBERS");
        sortCombo.addItem("WITHIN 100 M");
        sortCombo.addItem("WITHIN 500 M");
        sortCombo.addItem("WITHIN 1 KM");
        sortCombo.setPreferredSize(new Dimension(140, 25));
        sortCombo.setFont(new Font("Arial", Font.PLAIN, 10));
        sortCombo.addActionListener(this);

        controlsPanel.add(listBtn);
        controlsPanel.add(gridBtn);
        controlsPanel.add(sortCombo);

        add(controlsPanel);

        // Groups list
        createGroupsList();
    }

    private void updateGroupsList() {
        createGroupsContent();
        updateMapWaypoints();
    }

    private List<StudyLocation> getFilteredAndSortedLocations() {
        List<StudyLocation> filtered = new ArrayList<>(studyLocations);
        String selectedSort = (String) sortCombo.getSelectedItem();

        if (selectedSort == null) selectedSort = "SORT BY DISTANCE";

        // Apply distance filters (converted to meters)
        if (selectedSort.equals("WITHIN 100 M")) {
            filtered.removeIf(location -> location.getDistance() > 100);
        } else if (selectedSort.equals("WITHIN 500 M")) {
            filtered.removeIf(location -> location.getDistance() > 500);
        } else if (selectedSort.equals("WITHIN 1 KM")) {
            filtered.removeIf(location -> location.getDistance() > 1000);
        }

        // Apply sorting
        if (selectedSort.equals("SORT BY DISTANCE") || selectedSort.startsWith("WITHIN")) {
            filtered.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));
        } else if (selectedSort.equals("SORT BY NAME")) {
            filtered.sort((a, b) -> a.getGroupName().compareToIgnoreCase(b.getGroupName()));
        } else if (selectedSort.equals("SORT BY MEMBERS")) {
            filtered.sort((a, b) -> Integer.compare(b.getMemberCount(), a.getMemberCount()));
        }

        return filtered;
    }

    private void createGroupsList() {
        createGroupsContent();
    }

    private void createGroupsContent() {
        if (groupsScrollPane != null) {
            remove(groupsScrollPane);
            groupsScrollPane = null;
        }

        if (isGridView) {
            createGridView();
        } else {
            createListView();
        }

        revalidate();
        repaint();
    }

    private void createListView() {
        JPanel scrollableContent = new JPanel();
        scrollableContent.setLayout(null);
        scrollableContent.setBackground(GREY_BACKGROUND);

        List<StudyLocation> locations = getFilteredAndSortedLocations();

        int itemHeight = 70;
        int contentHeight = Math.max(locations.size() * itemHeight + 20, 150);
        scrollableContent.setPreferredSize(new Dimension(810, contentHeight));

        for (int i = 0; i < locations.size(); i++) {
            StudyLocation location = locations.get(i);
            String locationText = formatDistance(location.getDistance()) + " - " + location.getBuilding();
            String membersText = location.getMemberCount() + " MEMBERS";

            createGroupItem(scrollableContent, location.getGroupName(), locationText, membersText,
                    10, 10 + (i * itemHeight));
        }

        int scrollPaneY = mapExpanded ? 410 + (expandedMapHeight - normalMapHeight) : 410;

        groupsScrollPane = new JScrollPane(scrollableContent);
        groupsScrollPane.setBounds(20, scrollPaneY, 830, 180);
        groupsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        groupsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        groupsScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(groupsScrollPane);
    }

    private void createGridView() {
        JPanel scrollableContent = new JPanel();
        scrollableContent.setLayout(null);
        scrollableContent.setBackground(GREY_BACKGROUND);

        List<StudyLocation> locations = getFilteredAndSortedLocations();

        int columns = 3;
        int rows = (int) Math.ceil((double) locations.size() / columns);
        int cardWidth = 260;
        int cardHeight = 120;
        int padding = 10;

        int contentHeight = Math.max((rows * cardHeight) + ((rows + 1) * padding), 180);
        scrollableContent.setPreferredSize(new Dimension(810, contentHeight));

        for (int i = 0; i < locations.size(); i++) {
            StudyLocation location = locations.get(i);
            int row = i / columns;
            int col = i % columns;
            int x = padding + (col * (cardWidth + padding));
            int y = padding + (row * (cardHeight + padding));

            String locationText = formatDistance(location.getDistance()) + " - " + location.getBuilding();
            String membersText = location.getMemberCount() + " MEMBERS";

            createGridItem(scrollableContent, location.getGroupName(), locationText, membersText,
                    x, y, cardWidth, cardHeight);
        }

        int scrollPaneY = mapExpanded ? 410 + (expandedMapHeight - normalMapHeight) : 410;

        groupsScrollPane = new JScrollPane(scrollableContent);
        groupsScrollPane.setBounds(20, scrollPaneY, 830, 180);
        groupsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        groupsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        groupsScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(groupsScrollPane);
    }

    private void createGridItem(JPanel parent, String groupName, String location, String members, int x, int y, int width, int height) {
        JPanel itemPanel = new JPanel();
        itemPanel.setBounds(x, y, width, height);
        itemPanel.setBackground(Color.LIGHT_GRAY);
        itemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        itemPanel.setLayout(null);

        JPanel iconPanel = new JPanel();
        iconPanel.setBounds(10, 10, 50, 50);
        iconPanel.setBackground(Color.GRAY);
        iconPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        itemPanel.add(iconPanel);

        JLabel nameLabel = new JLabel(groupName);
        nameLabel.setBounds(70, 10, 180, 15);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));
        itemPanel.add(nameLabel);

        JLabel locationLabel = new JLabel("• " + location);
        locationLabel.setBounds(70, 25, 180, 12);
        locationLabel.setFont(new Font("Arial", Font.PLAIN, 9));
        itemPanel.add(locationLabel);

        JLabel membersLabel = new JLabel(members);
        membersLabel.setBounds(70, 37, 100, 12);
        membersLabel.setFont(new Font("Arial", Font.PLAIN, 9));
        itemPanel.add(membersLabel);

        JButton messageBtn = new JButton("MESSAGE");
        messageBtn.setBounds(10, 70, 100, 20);
        messageBtn.setBackground(BUTTON_BLUE);
        messageBtn.setForeground(Color.BLACK);
        messageBtn.setFont(new Font("Arial", Font.PLAIN, 8));
        messageBtn.setBorder(BorderFactory.createEmptyBorder());
        messageBtn.setFocusPainted(false);
        messageBtn.setActionCommand("MESSAGE");
        messageBtn.addActionListener(this);
        itemPanel.add(messageBtn);

        parent.add(itemPanel);
    }

    private void switchToListView() {
        if (isGridView) {
            isGridView = false;
            listBtn.setBackground(BUTTON_BLUE);
            listBtn.setForeground(Color.WHITE);
            gridBtn.setBackground(Color.WHITE);
            gridBtn.setForeground(Color.BLACK);
            createGroupsContent();
        }
    }

    private void switchToGridView() {
        if (!isGridView) {
            isGridView = true;
            gridBtn.setBackground(BUTTON_BLUE);
            gridBtn.setForeground(Color.WHITE);
            listBtn.setBackground(Color.WHITE);
            listBtn.setForeground(Color.BLACK);
            createGroupsContent();
        }
    }

    private void toggleMapSize() {
        mapExpanded = !mapExpanded;

        if (mapExpanded) {
            mapPanel.setBounds(20, 155, 830, expandedMapHeight);
            setSize(890, expandedFrameHeight);
            expandButton.setText("COLLAPSE");
            expandButton.setActionCommand("COLLAPSE");

            Component[] components = getContentPane().getComponents();
            for (Component comp : components) {
                if (comp.getBounds().y >= 375) {
                    Rectangle bounds = comp.getBounds();
                    bounds.y += (expandedMapHeight - normalMapHeight);
                    comp.setBounds(bounds);
                }
            }

        } else {
            mapPanel.setBounds(20, 155, 830, normalMapHeight);
            setSize(890, normalFrameHeight);
            expandButton.setText("EXPAND");
            expandButton.setActionCommand("EXPAND");

            Component[] components = getContentPane().getComponents();
            for (Component comp : components) {
                if (comp.getBounds().y > 375) {
                    Rectangle bounds = comp.getBounds();
                    bounds.y -= (expandedMapHeight - normalMapHeight);
                    comp.setBounds(bounds);
                }
            }
        }

        revalidate();
        repaint();
    }

    private void createGroupItem(JPanel parent, String groupName, String location, String members, int x, int y) {
        JPanel itemPanel = new JPanel();
        itemPanel.setBounds(x, y, 790, 60);
        itemPanel.setBackground(Color.LIGHT_GRAY);
        itemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        itemPanel.setLayout(null);

        JPanel iconPanel = new JPanel();
        iconPanel.setBounds(10, 10, 40, 40);
        iconPanel.setBackground(Color.GRAY);
        iconPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        itemPanel.add(iconPanel);

        JLabel nameLabel = new JLabel(groupName);
        nameLabel.setBounds(60, 10, 250, 15);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        itemPanel.add(nameLabel);

        JLabel locationLabel = new JLabel("• " + location);
        locationLabel.setBounds(60, 25, 250, 12);
        locationLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        itemPanel.add(locationLabel);

        JLabel membersLabel = new JLabel(members);
        membersLabel.setBounds(60, 37, 150, 12);
        membersLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        itemPanel.add(membersLabel);

        JButton messageBtn = new JButton("MESSAGE");
        messageBtn.setBounds(645, 20, 100, 20);
        messageBtn.setBackground(BUTTON_BLUE);
        messageBtn.setForeground(Color.BLACK);
        messageBtn.setFont(new Font("Arial", Font.PLAIN, 10));
        messageBtn.setBorder(BorderFactory.createEmptyBorder());
        messageBtn.setFocusPainted(false);
        messageBtn.setActionCommand("MESSAGE");
        messageBtn.addActionListener(this);
        itemPanel.add(messageBtn);

        parent.add(itemPanel);
    }

    private void createTrademark() {
        JPanel trademarkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        int trademarkY = mapExpanded ? 760 : 610;
        trademarkPanel.setBounds(0, trademarkY, 870, 30);
        trademarkPanel.setBackground(Color.WHITE);
        trademarkPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 0, 5, 0)
        ));

        JLabel trademarkLabel = new JLabel("© 2025 LearnHub - All Rights Reserved");
        trademarkLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        trademarkLabel.setForeground(Color.GRAY);

        trademarkPanel.add(trademarkLabel);
        add(trademarkPanel);
    }

    // ---------------- Utility ----------------

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radius of the earth in meters

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // Distance in meters

        return distance;
    }

    private String formatDistance(double distanceInMeters) {
        if (distanceInMeters < 1000) {
            return String.format("%.0f M", distanceInMeters);
        } else {
            return String.format("%.2f KM", distanceInMeters / 1000);
        }
    }

    // ---------------- Action handling ----------------

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

    if (command == null)
        return;

    if (command.equals("EXPAND") || command.equals("COLLAPSE")) {
        toggleMapSize();
    } else if (command.equals("LIST")) {
        switchToListView();
    } else if (command.equals("GRID")) {
        switchToGridView();
    } else if (command.equals("MESSAGE")) {
        showUnderConstructionDialog("Messaging Feature");
    } else if (command.equals("NAV_DASHBOARD")) {
        showUnderConstructionDialog("Dashboard");
    } else if (command.equals("NAV_PROFILE")) {
        showUnderConstructionDialog("Profile");
    } else if (command.equals("NAV_GROUPS")) {
        showUnderConstructionDialog("Groups");
    } else if (command.equals("NAV_MESSAGES")) {
        showUnderConstructionDialog("Messages");
    } else if (command.equals("NAV_STUDY_POINTS")) {
        showUnderConstructionDialog("Study Points");
    } else if (command.equals("NAV_LOCATION")) {
        centerOnUser();
    } else if (e.getSource() == sortCombo) {
        updateGroupsList();
    }
    }

    private void showUnderConstructionDialog(String featureName) {
        JOptionPane.showMessageDialog(
                this,
                featureName + " is currently under construction.\nThis feature will be available in a future update.",
                "Under Construction",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ---------------- Optional DB helper for storing location (unused if dbHelper used) ----------------
    // kept here in case you want local direct SQL usage in this class

    private boolean saveUserLocationToDatabase(GeoPosition position) {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            con = DriverManager.getConnection("jdbc:derby://localhost:1527/LocationDB;user=app;password=app");

            // Create table if not exists
            createUserLocationTableIfNotExists(con);

            // Use DELETE + INSERT approach
            String deleteSql = "DELETE FROM user_location WHERE id = 1";
            stmt = con.prepareStatement(deleteSql);
            stmt.executeUpdate();
            stmt.close();

            String insertSql = "INSERT INTO user_location (id, latitude, longitude) VALUES (1, ?, ?)";
            stmt = con.prepareStatement(insertSql);
            stmt.setDouble(1, position.getLatitude());
            stmt.setDouble(2, position.getLongitude());

            int rowsAffected = stmt.executeUpdate();
            System.out.println("User location saved: Lat=" + position.getLatitude() +
                    ", Lon=" + position.getLongitude());

            return rowsAffected > 0;

        } catch (Exception e) {
            System.err.println("Error saving location: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try { if (stmt != null) stmt.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }

    /**
     * Creates user_location table if not exists
     */
    private void createUserLocationTableIfNotExists(Connection con) throws SQLException {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            String createTableSQL =
                    "CREATE TABLE user_location (" +
                            "id INT PRIMARY KEY, " +
                            "latitude DOUBLE NOT NULL, " +
                            "longitude DOUBLE NOT NULL, " +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ")";
            stmt.executeUpdate(createTableSQL);
            System.out.println("Created user_location table");
        } catch (SQLException e) {
            // X0Y32 is Derby SQLState for "table already exists"
            if (!"X0Y32".equals(e.getSQLState())) {
                throw e;
            }
        } finally {
            try { if (stmt != null) stmt.close(); } catch (Exception e) {}
        }
    }

    // ---------------- Main ----------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MapApp());
    }
}

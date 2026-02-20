package za.ac.cput.mapapp;

/**
 * 
 * @author abong
 */
public class Student {
    private int id;
    private String firstName;
    private String lastName;
    private String studentNumber;
    private String email;
    private String course;
    
    // Constructor
    public Student(int id, String firstName, String lastName, String studentNumber, 
                  String email, String course) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.studentNumber = studentNumber;
        this.email = email;
        this.course = course;
    }
    
    // Default constructor
    public Student() {
        this.id = 0;
        this.firstName = "User";
        this.lastName = "";
        this.studentNumber = "231234567";
        this.email = "example@cput.ac.za";
        this.course = "Computer Science";
    }
    
    // Getters
    public int getId() {
        return id;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getFullName() {
        return firstName + (lastName.isEmpty() ? "" : " " + lastName);
    }
    
    public String getStudentNumber() {
        return studentNumber;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getCourse() {
        return course;
    }
    
    public char getFirstInitial() {
        return firstName != null && !firstName.isEmpty() ? 
               Character.toUpperCase(firstName.charAt(0)) : 'U';
    }
    
    // Setters
    public void setId(int id) {
        this.id = id;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setCourse(String course) {
        this.course = course;
    }
    
    @Override
    public String toString() {
        return String.format("Student{id=%d, firstName='%s', lastName='%s', " +
                           "studentNumber='%s', email='%s', course='%s'}", 
                           id, firstName, lastName, studentNumber, email, course);
    }
}
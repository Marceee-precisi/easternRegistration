package com.example;

import java.util.ArrayList;
import java.util.List;

/**
 * A student and the course sections they have added to their schedule.
 */
public class Student {
    /** The maximum credit load allowed for this project. */
    public static final int MAX_CREDITS = 17;

    private String studentId;
    private String firstName;
    private String lastName;
    private final List<Course> registeredCourses;

    /**
     * Creates an empty student object.
     */
    public Student() {
        this.studentId = "";
        this.firstName = "";
        this.lastName = "";
        this.registeredCourses = new ArrayList<>();
    }

    /**
     * Creates a student with the supplied identity.
     *
     * @param studentId the student's ID
     * @param firstName the student's first name
     * @param lastName the student's last name
     */
    public Student(String studentId, String firstName, String lastName) {
        this.studentId = requireText(studentId, "Student ID");
        this.firstName = requireText(firstName, "First name");
        this.lastName = requireText(lastName, "Last name");
        this.registeredCourses = new ArrayList<>();
    }

    /**
     * Returns the student's ID.
     *
     * @return the student ID
     */
    public String getStudentId() {
        return studentId;
    }

    /**
     * Returns the student's first name.
     *
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Returns the student's last name.
     *
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Returns a read-only snapshot of the student's registered courses.
     *
     * @return the registered course sections
     */
    public List<Course> getRegisteredCourses() {
        return List.copyOf(registeredCourses);
    }

    /**
     * Returns the current credit load.
     *
     * @return the sum of the courses' credit values
     */
    public double getCreditsEnrolled() {
        return registeredCourses.stream().mapToDouble(Course::creditsForLimit).sum();
    }

    /**
     * Adds a section if it satisfies the student's registration rules.
     *
     * @param course the section to add
     * @return the result explaining whether it was added
     */
    public RegistrationResult addCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course must not be null.");
        }
        if (containsCourse(course)) {
            return new RegistrationResult(RegistrationStatus.ALREADY_REGISTERED,
                "This section is already on the schedule.");
        }
        if (!course.isOpen()) {
            return new RegistrationResult(RegistrationStatus.COURSE_FULL,
                "This section has no available seats.");
        }
        if (course.creditsForLimit() <= 0) {
            return new RegistrationResult(RegistrationStatus.CREDIT_VALUE_UNKNOWN,
                "The section's credit value is not available, so it cannot be added safely.");
        }
        if (getCreditsEnrolled() + course.creditsForLimit() > MAX_CREDITS) {
            return new RegistrationResult(RegistrationStatus.CREDIT_LIMIT_EXCEEDED,
                "Adding this section would exceed the " + MAX_CREDITS + "-credit limit.");
        }
        for (Course registeredCourse : registeredCourses) {
            if (registeredCourse.conflictsWith(course)) {
                return new RegistrationResult(RegistrationStatus.TIME_CONFLICT,
                    "This section conflicts with " + registeredCourse + ".");
            }
        }

        registeredCourses.add(course);
        return new RegistrationResult(RegistrationStatus.ADDED,
            "Section added to the schedule.");
    }

    /**
     * Attempts to add multiple sections in order.
     *
     * @param courses the sections to add
     * @return one result for each section, in input order
     */
    public List<RegistrationResult> addCourses(List<Course> courses) {
        if (courses == null) {
            throw new IllegalArgumentException("Courses must not be null.");
        }
        List<RegistrationResult> results = new ArrayList<>();
        for (Course course : courses) {
            results.add(addCourse(course));
        }
        return List.copyOf(results);
    }

    /**
     * Removes a section from the student's schedule.
     *
     * @param course the section to remove
     * @return true if a matching section was removed
     */
    public boolean dropCourse(Course course) {
        if (course == null) {
            return false;
        }
        return registeredCourses.removeIf(registeredCourse -> sameSection(registeredCourse, course));
    }

    /**
     * Removes every section from the student's schedule.
     *
     * @return the number of sections removed
     */
    public int dropAllCourses() {
        int removed = registeredCourses.size();
        registeredCourses.clear();
        return removed;
    }

    private boolean containsCourse(Course course) {
        return registeredCourses.stream().anyMatch(registeredCourse -> sameSection(registeredCourse, course));
    }

    private static boolean sameSection(Course first, Course second) {
        if (hasText(first.courseReferenceNumber()) && hasText(second.courseReferenceNumber())) {
            return first.courseReferenceNumber().equals(second.courseReferenceNumber());
        }
        if (hasText(first.id()) && hasText(second.id())) {
            return first.id().equals(second.id());
        }
        return first.equals(second);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank.");
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * The outcome of attempting to add a course section.
     *
     * @param status the outcome category
     * @param message a user-readable explanation
     */
    public record RegistrationResult(RegistrationStatus status, String message) {}

    /** Possible outcomes of a course registration attempt. */
    public enum RegistrationStatus {
        /** The section was added to the schedule. */
        ADDED,
        /** The section is already registered. */
        ALREADY_REGISTERED,
        /** The section has no open seats. */
        COURSE_FULL,
        /** The section has no usable credit value. */
        CREDIT_VALUE_UNKNOWN,
        /** The addition would exceed the student's credit limit. */
        CREDIT_LIMIT_EXCEEDED,
        /** The section overlaps another registered course. */
        TIME_CONFLICT
    }
}

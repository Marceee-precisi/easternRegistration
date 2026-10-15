package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.DayOfWeek;
import java.util.List;

import org.junit.Test;

/**
 * Tests student schedule and registration rules.
 */
public class StudentTest {
    @Test
    public void rejectsSectionsWithoutOpenSeats() {
        Student student = new Student("123", "Ada", "Lovelace");

        Student.RegistrationResult result = student.addCourse(course(
            "full", 3, 0, List.of()));

        assertEquals(Student.RegistrationStatus.COURSE_FULL, result.status());
        assertTrue(student.getRegisteredCourses().isEmpty());
    }

    @Test
    public void enforcesCreditLimit() {
        Student student = new Student("123", "Ada", "Lovelace");
        for (int index = 0; index < 5; index++) {
            assertEquals(Student.RegistrationStatus.ADDED,
                student.addCourse(course("three-" + index, 3, 10, List.of())).status());
        }
        assertEquals(Student.RegistrationStatus.ADDED,
            student.addCourse(course("one", 1, 10, List.of())).status());
        assertEquals(Student.RegistrationStatus.CREDIT_LIMIT_EXCEEDED,
            student.addCourse(course("two", 2, 10, List.of())).status());
        assertEquals(16.0, student.getCreditsEnrolled(), 0.0);
    }

    @Test
    public void rejectsOverlappingMeetingsButAllowsBackToBackMeetings() {
        Student student = new Student("123", "Ada", "Lovelace");
        Course morning = course("morning", 3, 10,
            List.of(meeting(true, false, false, false, false, "0900", "1000",
                "08/25/2026", "12/13/2026")));
        Course overlap = course("overlap", 3, 10,
            List.of(meeting(true, false, false, false, false, "0930", "1030",
                "08/25/2026", "12/13/2026")));
        Course backToBack = course("back-to-back", 3, 10,
            List.of(meeting(true, false, false, false, false, "1000", "1100",
                "08/25/2026", "12/13/2026")));

        assertEquals(Student.RegistrationStatus.ADDED, student.addCourse(morning).status());
        assertEquals(Student.RegistrationStatus.TIME_CONFLICT, student.addCourse(overlap).status());
        assertEquals(Student.RegistrationStatus.ADDED, student.addCourse(backToBack).status());
    }

    @Test
    public void considersEveryMeetingAndItsDateRange() {
        Student student = new Student("123", "Ada", "Lovelace");
        Course laboratory = course("lab", 2, 10, List.of(
            meeting(false, true, false, false, false, "1300", "1400",
                "08/25/2026", "10/01/2026"),
            meeting(false, false, false, true, false, "1300", "1400",
                "10/02/2026", "12/13/2026")));
        Course sameTimeLaterDates = course("later", 2, 10, List.of(
            meeting(false, true, false, false, false, "1300", "1400",
                "10/02/2026", "12/13/2026")));

        assertTrue(laboratory.meetsOn(DayOfWeek.TUESDAY));
        assertTrue(laboratory.meetsOn(DayOfWeek.THURSDAY));
        assertEquals(Student.RegistrationStatus.ADDED, student.addCourse(laboratory).status());
        assertEquals(Student.RegistrationStatus.ADDED, student.addCourse(sameTimeLaterDates).status());
        Course thursdayOverlap = course("thursday-overlap", 2, 10, List.of(
            meeting(false, false, false, true, false, "1330", "1400",
                "10/02/2026", "12/13/2026")));
        assertEquals(Student.RegistrationStatus.TIME_CONFLICT,
            student.addCourse(thursdayOverlap).status());
    }

    @Test
    public void rejectsDuplicateSectionsAndSupportsDropping() {
        Student student = new Student("123", "Ada", "Lovelace");
        Course section = course("crn-101", 3, 10, List.of());

        assertEquals(Student.RegistrationStatus.ADDED, student.addCourse(section).status());
        assertEquals(Student.RegistrationStatus.ALREADY_REGISTERED,
            student.addCourse(section).status());
        assertTrue(student.dropCourse(section));
        assertFalse(student.dropCourse(section));
        assertEquals(Student.RegistrationStatus.ADDED,
            student.addCourse(course("crn-102", 3, 10, List.of())).status());
        assertEquals(1, student.dropAllCourses());
        assertEquals(0, student.dropAllCourses());
    }

    @Test
    public void countsTheMaximumOfVariableCreditCourses() {
        Course variableCreditCourse = new Course(
            "variable", "CSC", "390", "Special Topics", null, 1.0, 4.0,
            20, 0, 20, true, "variable-crn", "01", List.of(), List.of());
        Student student = new Student("123", "Ada", "Lovelace");

        assertEquals("1-4", variableCreditCourse.creditsLabel());
        assertEquals(4.0, variableCreditCourse.creditsForLimit(), 0.0);
        assertEquals(Student.RegistrationStatus.ADDED,
            student.addCourse(variableCreditCourse).status());
        assertEquals(4.0, student.getCreditsEnrolled(), 0.0);
    }

    private static Course course(String crn, double credits, int seats, List<Course.MeetingFaculty> meetings) {
        return new Course(crn, "CSC", "101", "Test Course", credits, null, null,
            20, 20 - seats, seats, seats > 0, crn, "01", List.of(), meetings);
    }

    private static Course.MeetingFaculty meeting(
        boolean monday,
        boolean tuesday,
        boolean wednesday,
        boolean thursday,
        boolean friday,
        String start,
        String end,
        String startDate,
        String endDate
    ) {
        return new Course.MeetingFaculty(new Course.MeetingTime(
            monday, tuesday, wednesday, thursday, friday, false, false,
            start, end, null, null, startDate, endDate));
    }
}

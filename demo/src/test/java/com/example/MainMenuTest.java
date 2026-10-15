package com.example;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;

/**
 * Exercises the console menu with deterministic schedule data and input.
 */
public class MainMenuTest {
    @Test
    public void filtersAddsDisplaysAndDropsASection() throws Exception {
        Course mondayCourse = course("10946", "01", "Ada Lovelace",
            true, false, true);
        Course wrongInstructor = course("10947", "02", "Bob Example",
            true, false, true);
        Course excludedWeekday = course("10948", "03", "Ada Lovelace",
            false, true, true);
        Course fullSection = course("10949", "04", "Ada Lovelace",
            true, false, false);
        EasternSchedule schedule = new EasternSchedule() {
            @Override
            public ArrayList<Course> queryCourseBySubject(String subject) {
                assertTrue(subject.equals("CSC"));
                return new ArrayList<>(List.of(
                    mondayCourse, wrongInstructor, excludedWeekday, fullSection));
            }
        };
        String input = String.join("\n",
            "123", "Ada", "Lovelace",
            "3", "CSC", "360", "y", "Ada", "Tue", "1",
            "5", "4", "1", "5", "6", "");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try (PrintStream capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
             Scanner scanner = new Scanner(input)) {
            System.setOut(capturedOutput);
            new MainMenu(schedule, scanner).open();
        } finally {
            System.setOut(originalOutput);
        }

        String transcript = output.toString(StandardCharsets.UTF_8);
        assertTrue(transcript.contains("1. CSC 360-01"));
        assertTrue(transcript.contains("Section added to the schedule."));
        assertTrue(transcript.contains("Credits: 3 / 17"));
        assertTrue(transcript.contains("Dropped CSC 360."));
        assertTrue(transcript.contains("No courses registered."));
        assertFalse(transcript.contains("Bob Example"));
        assertFalse(transcript.contains("CSC 360-03"));
        assertFalse(transcript.contains("CSC 360-04"));
    }

    private static Course course(
        String crn,
        String section,
        String instructor,
        boolean monday,
        boolean tuesday,
        boolean open
    ) {
        Course.MeetingTime time = new Course.MeetingTime(
            monday, tuesday, false, false, false, false, false,
            "0900", "1000", null, null, "08/25/2026", "12/13/2026");
        return new Course(crn, "CSC", "360", "Software Development with AI",
            3.0, null, null, 20, open ? 10 : 20, open ? 10 : 0, open,
            crn, section, List.of(new Course.Faculty(instructor)),
            List.of(new Course.MeetingFaculty(time)));
    }
}

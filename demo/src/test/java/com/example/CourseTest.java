package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/**
 * Tests mapping and formatting of schedule-service course data.
 */
public class CourseTest {
    @Test
    public void mapsRequiredCourseAndMeetingDetailsFromJson() throws Exception {
        String json = """
            {
              "id": 103267,
              "subject": "CSC",
              "courseNumber": "360",
              "courseTitle": "Software Development with AI",
              "creditHours": null,
              "creditHourLow": 3,
              "creditHourHigh": null,
              "courseReferenceNumber": "10946",
              "sequenceNumber": "02",
              "seatsAvailable": 2,
              "openSection": true,
              "faculty": [{"displayName": "Ada Lovelace"}],
              "meetingsFaculty": [
                {
                  "meetingTime": {
                    "monday": true,
                    "tuesday": false,
                    "wednesday": true,
                    "thursday": false,
                    "friday": true,
                    "beginTime": "1400",
                    "endTime": "1515",
                    "buildingDescription": "Science Building",
                    "room": "101",
                    "startDate": "08/25/2026",
                    "endDate": "12/13/2026"
                  }
                }
              ]
            }
            """;

        Course course = new ObjectMapper().readValue(json, Course.class);

        assertEquals("CSC 360-02, 3 credits, Software Development with AI, "
            + "MWF, 2:00 PM - 3:15 PM, Science Building 101, "
            + "Ada Lovelace, 2 seats available (CRN 10946)", course.toString());
        assertTrue(course.isOpen());
        assertTrue(course.meetsOn(java.time.DayOfWeek.WEDNESDAY));
        assertFalse(course.meetsOn(java.time.DayOfWeek.THURSDAY));
    }

    @Test
    public void reportsOpenSeatsAsTheSourceOfAvailability() {
        Course noSeats = new Course("id", "CSC", "101", "Course",
            3.0, null, null, 20, 20, 0, true, "12345", "01", null, null);

        assertFalse(noSeats.isOpen());
        assertEquals("Time TBA", noSeats.meetingsLabel());
        assertEquals("Instructor TBA", noSeats.instructorsLabel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidSubjectsBeforeBuildingCachePaths() throws Exception {
        new EasternSchedule().queryCourseBySubject("../CSC");
    }
}

package com.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A catalog section and the information needed to display and register for it.
 *
 * @param id the schedule-system identifier
 * @param subject the subject abbreviation
 * @param courseNumber the catalog number
 * @param courseTitle the title of the course
 * @param creditHours the fixed credit value, if provided
 * @param creditHourLow the minimum credits, if provided
 * @param creditHourHigh the maximum credits, if provided
 * @param maximumEnrollment the section capacity
 * @param enrollment the current enrollment
 * @param seatsAvailable the number of remaining seats
 * @param openSection whether the schedule system marks the section open
 * @param courseReferenceNumber the section's registration number
 * @param sequenceNumber the section number
 * @param faculty the instructors associated with the section
 * @param meetingsFaculty the section's meeting patterns
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Course(
    String id,
    String subject,
    String courseNumber,
    String courseTitle,
    Double creditHours,
    Double creditHourLow,
    Double creditHourHigh,
    Integer maximumEnrollment,
    Integer enrollment,
    Integer seatsAvailable,
    Boolean openSection,
    String courseReferenceNumber,
    String sequenceNumber,
    List<Faculty> faculty,
    List<MeetingFaculty> meetingsFaculty
) {
    private static final DateTimeFormatter API_TIME_FORMAT =
        DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
        DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    /**
     * Normalizes absent collections in API data to immutable empty lists.
     */
    public Course {
        faculty = faculty == null ? List.of() : List.copyOf(faculty);
        meetingsFaculty = meetingsFaculty == null ? List.of() : List.copyOf(meetingsFaculty);
    }

    /**
     * Instructor information returned by the schedule service.
     *
     * @param displayName the instructor's display name
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Faculty(String displayName) {}

    /**
     * A meeting wrapper used by the schedule service.
     *
     * @param meetingTime the days, times, and location for this meeting
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingFaculty(MeetingTime meetingTime) {
        /**
         * Returns the formatted meeting pattern.
         *
         * @return the days and time, or a TBA indication
         */
        @Override
        public String toString() {
            return meetingTime == null ? "Time TBA" : meetingTime.toString();
        }
    }

    /**
     * The weekly pattern and location for one course meeting.
     *
     * @param monday true if this meeting occurs on Monday
     * @param tuesday true if this meeting occurs on Tuesday
     * @param wednesday true if this meeting occurs on Wednesday
     * @param thursday true if this meeting occurs on Thursday
     * @param friday true if this meeting occurs on Friday
     * @param saturday true if this meeting occurs on Saturday
     * @param sunday true if this meeting occurs on Sunday
     * @param beginTime the start time in the schedule service's HHmm format
     * @param endTime the end time in the schedule service's HHmm format
     * @param buildingDescription the building's display name
     * @param room the room number
     * @param startDate the first date of this meeting pattern
     * @param endDate the last date of this meeting pattern
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingTime(
        boolean monday,
        boolean tuesday,
        boolean wednesday,
        boolean thursday,
        boolean friday,
        boolean saturday,
        boolean sunday,
        String beginTime,
        String endTime,
        String buildingDescription,
        String room,
        String startDate,
        String endDate
    ) {
        /**
         * Returns the days on which this meeting occurs.
         *
         * @return a compact weekday string, or TBA when no day is listed
         */
        public String daysLabel() {
            StringBuilder days = new StringBuilder();
            appendDay(days, monday, "M");
            appendDay(days, tuesday, "T");
            appendDay(days, wednesday, "W");
            appendDay(days, thursday, "R");
            appendDay(days, friday, "F");
            appendDay(days, saturday, "Sa");
            appendDay(days, sunday, "Su");
            return days.isEmpty() ? "Days TBA" : days.toString();
        }

        /**
         * Returns true when this meeting overlaps another on a shared weekday.
         *
         * @param other the meeting to compare against
         * @return true if the meetings share a day and their date/time ranges overlap
         */
        public boolean conflictsWith(MeetingTime other) {
            if (other == null || !sharesDayWith(other)) {
                return false;
            }

            LocalTime thisStart = parseTime(beginTime);
            LocalTime thisEnd = parseTime(endTime);
            LocalTime otherStart = parseTime(other.beginTime);
            LocalTime otherEnd = parseTime(other.endTime);
            if (thisStart == null || thisEnd == null || otherStart == null || otherEnd == null) {
                return false;
            }

            return datesOverlap(other)
                && thisStart.isBefore(otherEnd)
                && otherStart.isBefore(thisEnd);
        }

        /**
         * Returns true when this meeting occurs on the requested weekday.
         *
         * @param day the day to check
         * @return true if this meeting occurs on the requested day
         */
        public boolean occursOn(java.time.DayOfWeek day) {
            return switch (day) {
                case MONDAY -> monday;
                case TUESDAY -> tuesday;
                case WEDNESDAY -> wednesday;
                case THURSDAY -> thursday;
                case FRIDAY -> friday;
                case SATURDAY -> saturday;
                case SUNDAY -> sunday;
            };
        }

        private boolean sharesDayWith(MeetingTime other) {
            return (monday && other.monday)
                || (tuesday && other.tuesday)
                || (wednesday && other.wednesday)
                || (thursday && other.thursday)
                || (friday && other.friday)
                || (saturday && other.saturday)
                || (sunday && other.sunday);
        }

        private boolean datesOverlap(MeetingTime other) {
            try {
                if (startDate == null || endDate == null
                    || other.startDate == null || other.endDate == null) {
                    return true;
                }
                java.time.format.DateTimeFormatter dateFormat =
                    java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy");
                java.time.LocalDate thisStart = java.time.LocalDate.parse(startDate, dateFormat);
                java.time.LocalDate thisEnd = java.time.LocalDate.parse(endDate, dateFormat);
                java.time.LocalDate otherStart = java.time.LocalDate.parse(other.startDate, dateFormat);
                java.time.LocalDate otherEnd = java.time.LocalDate.parse(other.endDate, dateFormat);
                return !thisStart.isAfter(otherEnd) && !otherStart.isAfter(thisEnd);
            } catch (DateTimeParseException exception) {
                return true;
            }
        }

        private static LocalTime parseTime(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return LocalTime.parse(value, API_TIME_FORMAT);
            } catch (DateTimeParseException exception) {
                return null;
            }
        }

        private static void appendDay(StringBuilder days, boolean included, String label) {
            if (included) {
                days.append(label);
            }
        }

        private String timeRangeLabel() {
            LocalTime start = parseTime(beginTime);
            LocalTime end = parseTime(endTime);
            if (start == null || end == null) {
                return "Time TBA";
            }
            return DISPLAY_TIME_FORMAT.format(start) + " - " + DISPLAY_TIME_FORMAT.format(end);
        }

        /**
         * Returns a readable description of the meeting pattern and location.
         *
         * @return the formatted meeting details
         */
        @Override
        public String toString() {
            StringBuilder label = new StringBuilder(daysLabel())
                .append(", ")
                .append(timeRangeLabel());
            if (buildingDescription != null && !buildingDescription.isBlank()) {
                label.append(", ").append(buildingDescription);
            }
            if (room != null && !room.isBlank()) {
                label.append(" ").append(room);
            }
            return label.toString();
        }
    }

    /**
     * Returns the credit value used for registration limit checks.
     *
     * @return the course's maximum stated credit value, or zero if unknown
     */
    public double creditsForLimit() {
        if (creditHours != null && creditHours > 0) {
            return creditHours;
        }
        if (creditHourHigh != null && creditHourHigh > 0) {
            return creditHourHigh;
        }
        if (creditHourLow != null && creditHourLow > 0) {
            return creditHourLow;
        }
        return 0;
    }

    /**
     * Returns a readable course-credit label.
     *
     * @return the credit value or range
     */
    public String creditsLabel() {
        if (creditHours != null && creditHours > 0) {
            return formatCredits(creditHours);
        }
        if (creditHourLow != null && creditHourLow > 0
            && creditHourHigh != null && creditHourHigh > creditHourLow) {
            return formatCredits(creditHourLow) + "-" + formatCredits(creditHourHigh);
        }
        double credits = creditsForLimit();
        return credits > 0 ? formatCredits(credits) : "Credit value TBA";
    }

    /**
     * Returns whether the section has at least one seat available.
     *
     * @return true if the schedule data indicates an open seat
     */
    public boolean isOpen() {
        if (seatsAvailable != null) {
            return seatsAvailable > 0;
        }
        return Boolean.TRUE.equals(openSection);
    }

    /**
     * Returns the listed instructor names.
     *
     * @return the instructor names, or Instructor TBA when none are listed
     */
    public String instructorsLabel() {
        List<String> names = new ArrayList<>();
        for (Faculty instructor : faculty) {
            if (instructor != null && instructor.displayName() != null
                && !instructor.displayName().isBlank()
                && !names.contains(instructor.displayName())) {
                names.add(instructor.displayName());
            }
        }
        return names.isEmpty() ? "Instructor TBA" : String.join(", ", names);
    }

    /**
     * Returns true when any meeting in this course occurs on the requested day.
     *
     * @param day the weekday to check
     * @return true if at least one meeting occurs on that day
     */
    public boolean meetsOn(java.time.DayOfWeek day) {
        return meetingsFaculty.stream()
            .filter(meeting -> meeting != null && meeting.meetingTime() != null)
            .anyMatch(meeting -> meeting.meetingTime().occursOn(day));
    }

    /**
     * Returns true when this section has at least one meeting that conflicts with another course.
     *
     * @param other the other section
     * @return true if any pair of meetings overlaps
     */
    public boolean conflictsWith(Course other) {
        if (other == null) {
            return false;
        }
        return meetingsFaculty.stream()
            .filter(meeting -> meeting != null && meeting.meetingTime() != null)
            .anyMatch(meeting -> other.meetingsFaculty.stream()
                .filter(otherMeeting -> otherMeeting != null && otherMeeting.meetingTime() != null)
                .anyMatch(otherMeeting ->
                    meeting.meetingTime().conflictsWith(otherMeeting.meetingTime())));
    }

    /**
     * Returns all meeting patterns for this section.
     *
     * @return the formatted meeting patterns, or Time TBA when no times are listed
     */
    public String meetingsLabel() {
        List<String> labels = meetingsFaculty.stream()
            .filter(meeting -> meeting != null)
            .map(MeetingFaculty::toString)
            .distinct()
            .toList();
        return labels.isEmpty() ? "Time TBA" : String.join("; ", labels);
    }

    /**
     * Returns a formatted summary of the section.
     *
     * @return the section summary with credits, title, meeting times, instructor, and seats
     */
    @Override
    public String toString() {
        String section = sequenceNumber == null || sequenceNumber.isBlank()
            ? courseNumber
            : courseNumber + "-" + sequenceNumber;
        String title = courseTitle == null || courseTitle.isBlank()
            ? "Course title TBA"
            : courseTitle;
        String credits = creditsLabel();
        String creditDescription = credits.equals("Credit value TBA")
            ? credits
            : credits + (credits.equals("1") ? " credit" : " credits");
        String seatLabel = seatsAvailable == null
            ? "Seats unknown"
            : seatsAvailable + (seatsAvailable == 1 ? " seat available" : " seats available");
        return subject + " " + section + ", " + creditDescription + ", "
            + title + ", " + meetingsLabel() + ", " + instructorsLabel()
            + ", " + seatLabel + " (CRN " + courseReferenceNumber + ")";
    }

    private static String formatCredits(double credits) {
        return credits == Math.rint(credits)
            ? Long.toString((long) credits)
            : Double.toString(credits);
    }
}

package com.example;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console interface for course browsing and student registration.
 */
public class MainMenu {
    private final EasternSchedule easternSchedule;
    private final Scanner scanner;
    private Student student;

    /**
     * Creates a menu connected to Eastern's schedule service and standard input.
     */
    public MainMenu() {
        this(new EasternSchedule(), new Scanner(System.in));
    }

    /**
     * Creates a menu using the supplied schedule service and input.
     *
     * @param easternSchedule the schedule service
     * @param scanner the source of console input
     */
    public MainMenu(EasternSchedule easternSchedule, Scanner scanner) {
        if (easternSchedule == null || scanner == null) {
            throw new IllegalArgumentException("Schedule and scanner must not be null.");
        }
        this.easternSchedule = easternSchedule;
        this.scanner = scanner;
    }

    /**
     * Opens the menu and handles user choices until the user exits or input ends.
     *
     * @throws Exception if schedule data cannot be retrieved
     */
    public void open() throws Exception {
        System.out.println("Eastern Registration — " + easternSchedule.getTerm());
        student = promptForStudent();
        if (student == null) {
            return;
        }

        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Choose an option: ");
            if (choice == null) {
                return;
            }
            switch (choice.trim()) {
                case "1" -> browseCourses();
                case "2" -> searchCourses();
                case "3" -> addCourse();
                case "4" -> dropCourses();
                case "5" -> displaySchedule();
                case "6" -> running = false;
                default -> System.out.println("Please choose a number from 1 to 6.");
            }
            System.out.println();
        }
        System.out.println("Goodbye.");
    }

    private Student promptForStudent() {
        System.out.println("Enter the student information for this session.");
        String id = promptRequired("Student ID: ");
        if (id == null) {
            return null;
        }
        String firstName = promptRequired("First name: ");
        if (firstName == null) {
            return null;
        }
        String lastName = promptRequired("Last name: ");
        if (lastName == null) {
            return null;
        }
        return new Student(id, firstName, lastName);
    }

    private void printMenu() {
        System.out.println("Student: " + student.getFirstName() + " "
            + student.getLastName() + " (" + student.getStudentId() + ")");
        System.out.println("1. Browse all courses by subject");
        System.out.println("2. Search by subject and course number");
        System.out.println("3. Add a course");
        System.out.println("4. Drop course(s)");
        System.out.println("5. Display student schedule");
        System.out.println("6. Exit");
    }

    private void browseCourses() throws Exception {
        String subject = promptRequired("Subject (for example, CSC): ");
        if (subject == null) {
            return;
        }
        List<Course> courses = easternSchedule.queryCourseBySubject(subject.trim().toUpperCase(Locale.ROOT));
        displayCourses(courses);
    }

    private void searchCourses() throws Exception {
        SearchResults results = findMatchingCourses();
        if (results != null) {
            displayCourses(results.courses());
        }
    }

    private void addCourse() throws Exception {
        SearchResults results = findMatchingCourses();
        if (results == null || results.courses().isEmpty()) {
            return;
        }

        displayCourses(results.courses());
        int selected = promptForCourseNumber(results.courses().size(),
            "Enter the result number to add (0 to cancel): ");
        if (selected <= 0) {
            System.out.println("No course added.");
            return;
        }
        Student.RegistrationResult result = student.addCourse(results.courses().get(selected - 1));
        System.out.println(result.message());
    }

    private SearchResults findMatchingCourses() throws Exception {
        String subject = promptRequired("Subject (for example, CSC): ");
        if (subject == null) {
            return null;
        }
        String courseNumber = promptRequired("Course number (for example, 360): ");
        if (courseNumber == null) {
            return null;
        }
        boolean onlyOpen = promptYesNo("Only sections with available seats? [y/N]: ");
        String instructor = prompt("Instructor name filter (leave blank for any): ");
        if (instructor == null) {
            return null;
        }
        Optional<DayOfWeek> excludedDay = promptExcludedDay();
        if (excludedDay == null) {
            return null;
        }

        String subjectCode = subject.trim().toUpperCase(Locale.ROOT);
        String number = courseNumber.trim();
        List<Course> matches = easternSchedule.queryCourseBySubject(subjectCode).stream()
            .filter(course -> course.courseNumber() != null
                && course.courseNumber().equalsIgnoreCase(number))
            .filter(course -> !onlyOpen || course.isOpen())
            .filter(course -> instructor.isBlank()
                || course.instructorsLabel().toLowerCase(Locale.ROOT)
                    .contains(instructor.trim().toLowerCase(Locale.ROOT)))
            .filter(course -> excludedDay.isEmpty()
                || !course.meetsOn(excludedDay.orElseThrow()))
            .toList();
        return new SearchResults(matches);
    }

    private void dropCourses() {
        List<Course> courses = student.getRegisteredCourses();
        if (courses.isEmpty()) {
            System.out.println("There are no courses to drop.");
            return;
        }
        displayCourses(courses);
        System.out.println("0. Drop all courses");
        int selected = promptForCourseNumber(courses.size(),
            "Enter a result number to drop, or 0 to drop all: ");
        if (selected < 0) {
            return;
        }
        if (selected == 0) {
            int removed = student.dropAllCourses();
            System.out.println("Dropped " + removed + (removed == 1 ? " course." : " courses."));
            return;
        }
        Course course = courses.get(selected - 1);
        if (student.dropCourse(course)) {
            System.out.println("Dropped " + course.subject() + " " + course.courseNumber() + ".");
        } else {
            System.out.println("That course was not on the schedule.");
        }
    }

    private void displaySchedule() {
        List<Course> courses = student.getRegisteredCourses();
        System.out.println("Schedule for " + student.getFirstName() + " " + student.getLastName());
        if (courses.isEmpty()) {
            System.out.println("No courses registered.");
        } else {
            displayCourses(courses);
        }
        System.out.printf(Locale.ROOT, "Credits: %s / %d%n",
            formatCredits(student.getCreditsEnrolled()), Student.MAX_CREDITS);
    }

    private void displayCourses(List<Course> courses) {
        if (courses.isEmpty()) {
            System.out.println("No matching courses found.");
            return;
        }
        for (int index = 0; index < courses.size(); index++) {
            System.out.println((index + 1) + ". " + courses.get(index));
        }
    }

    private int promptForCourseNumber(int maximum, String message) {
        while (true) {
            String input = prompt(message);
            if (input == null) {
                return -1;
            }
            try {
                int selected = Integer.parseInt(input.trim());
                if (selected >= 0 && selected <= maximum) {
                    return selected;
                }
            } catch (NumberFormatException exception) {
                System.out.println("Enter a whole number from 0 to " + maximum + ".");
                continue;
            }
            System.out.println("Enter a number from 0 to " + maximum + ".");
        }
    }

    private boolean promptYesNo(String message) {
        while (true) {
            String input = prompt(message);
            if (input == null || input.isBlank()) {
                return false;
            }
            if (input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes")) {
                return true;
            }
            if (input.equalsIgnoreCase("n") || input.equalsIgnoreCase("no")) {
                return false;
            }
            System.out.println("Please enter yes or no.");
        }
    }

    private Optional<DayOfWeek> promptExcludedDay() {
        while (true) {
            String input = prompt("Exclude a weekday (Mon, Tue, Wed, Thu, Fri, Sat, Sun; blank for none): ");
            if (input == null) {
                return null;
            }
            if (input.isBlank()) {
                return Optional.empty();
            }
            DayOfWeek day = parseDay(input);
            if (day != null) {
                return Optional.of(day);
            }
            System.out.println("Please enter a valid weekday or leave it blank.");
        }
    }

    private DayOfWeek parseDay(String value) {
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "MON", "MONDAY" -> DayOfWeek.MONDAY;
            case "T", "TUE", "TUES", "TUESDAY" -> DayOfWeek.TUESDAY;
            case "W", "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
            case "R", "TH", "THU", "THUR", "THURSDAY" -> DayOfWeek.THURSDAY;
            case "F", "FRI", "FRIDAY" -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY" -> DayOfWeek.SATURDAY;
            case "SU", "SUN", "SUNDAY" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private String promptRequired(String message) {
        while (true) {
            String input = prompt(message);
            if (input == null) {
                return null;
            }
            if (!input.isBlank()) {
                return input.trim();
            }
            System.out.println("This value is required.");
        }
    }

    private String prompt(String message) {
        System.out.print(message);
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    private static String formatCredits(double credits) {
        return credits == Math.rint(credits)
            ? Long.toString((long) credits)
            : Double.toString(credits);
    }

    private record SearchResults(List<Course> courses) {
        private SearchResults {
            courses = List.copyOf(courses);
        }
    }
}

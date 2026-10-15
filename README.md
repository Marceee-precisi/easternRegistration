# Eastern Registration

A Java console application for browsing Eastern Connecticut State University's course schedule and managing a student's sample schedule. The project uses the supplied Eastern schedule service and JSON cache; it does not submit registration changes to the university.

## What the application does

The application starts at `com.example.EasternRegistration` and opens a menu. At startup, enter a student ID, first name, and last name. The menu can:

1. List all sections for a subject.
2. Search a subject and course number, optionally limiting results to sections with open seats, an instructor name, or courses that do not meet on a selected weekday.
3. Add a section to the student's in-memory schedule.
4. Drop one section or all sections.
5. Display the student's schedule and current credit load.
6. Exit.

The default schedule term is Fall 2026 (`202710`). Student information and the student's schedule exist only for the current application run.

## Project layout

- `demo/src/main/java/com/example/` — Java application source.
  - `EasternRegistration.java` — application entry point.
  - `MainMenu.java` — console prompts, course searches, and schedule operations.
  - `Student.java` — student identity, schedule, and registration-rule checks.
  - `Course.java` — course, instructor, and meeting data, plus display and conflict helpers.
  - `Term.java` — available-term data.
  - `EasternSchedule.java` — schedule-service requests and JSON cache access.
- `demo/docs/easternRegistration.puml` — UML design for application classes and records.
- `demo/pom.xml` — Java 17 build configuration and Jackson dependency.
- `cache/` — local schedule snapshots; this directory is ignored by Git.
- `.github/workflows/compile.yml` — GitHub Actions compile workflow.

## Requirements and behavior

- A student can register for sections totaling no more than `Student.MAX_CREDITS` (17).
- A section must have seats available. A section with missing credit data is not added because its credit load cannot be checked safely.
- Duplicate registration and overlapping meetings are rejected. Meetings are compared by shared weekday, overlapping time, and overlapping meeting date ranges when dates are present. Back-to-back meetings are allowed.
- A course can have multiple meeting patterns. The search and display logic considers all of them.
- Credit ranges are displayed as ranges. For the 17-credit limit, a variable-credit course is counted at its maximum stated value.
- Capacity and availability are based on the schedule data when it was fetched or cached; the program does not reserve seats or change official registration.
- Course data is cached by term and subject. A cache hit avoids a network request; a cache miss requires access to Eastern's schedule service. The cache is not committed.

## Run the application

Java 17 or later and the **Extension Pack for Java** in Visual Studio Code are recommended. Open the folder containing this README, allow Java project dependencies to finish resolving, and select **Run** above `EasternRegistration.main`, or press **F5** and choose **Eastern Registration**. The launch configuration starts from the project root so the supplied `cache/` files can be found. The only `main` method in the submitted application is in `EasternRegistration`.

Maven is **not required just to run the project in the Java-aware VS Code setup**. Since the application uses Jackson, a Java setup still needs access to the Jackson dependency declared in `demo/pom.xml`; the Java extension can resolve dependencies from the Maven project configuration.

## Compile and check JavaDoc with Maven

Maven is recommended for verifying the build and is required for the assignment's JavaDoc generation check. From the project root, run:

```sh
mvn -f demo/pom.xml compile
mvn -f demo/pom.xml test
mvn -f demo/pom.xml javadoc:javadoc
```

The generated documentation is placed under `demo/target/reports/apidocs/`. It is build output and should not be committed.

To install Maven, download the binary distribution from [maven.apache.org/download.cgi](https://maven.apache.org/download.cgi), extract it, and add its `bin` directory to `PATH`. Confirm the installation with:

```sh
mvn -version
```

The GitHub Actions workflow uses JDK 17 and tests the project on pushes and pull requests. The Maven compiler targets Java 17 bytecode, so it can also run on the Java 21 runtime included with the VS Code Java extension.

## Project-work checklist

- [ ] Initialize and use a GitHub repository. The assignment requires the initial commit to contain the original starter files (excluding cache data), including `.gitignore` and `.vscode`.
- [ ] If the repository is private, invite the instructor as a collaborator.
- [ ] Push the initial version and confirm the GitHub Actions compile workflow passes.
- [ ] Develop features on focused branches, make logically scoped descriptive commits, and open/merge pull requests as appropriate.
- [ ] For AI-assisted commits, include the prompt used and state whether you made manual changes, as required by the assignment.
- [ ] Keep `demo/docs/easternRegistration.puml` aligned with the implemented classes.
- [ ] Include JavaDoc comments and run the JavaDoc generation command.
- [ ] Keep cache contents, generated JavaDocs, and other build output out of Git.

## Background

The instructor's [Project 1 background slides](https://gdancik.github.io/CSC-360/data/notes/Project1_Background.pdf) explain JSON objects and arrays, Jackson data binding into Java records, `TypeReference` for generic collections, and reading/writing cache files.

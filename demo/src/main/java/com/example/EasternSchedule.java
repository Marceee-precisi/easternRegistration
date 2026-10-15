package com.example;

import java.io.IOException;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.CookiePolicy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provides access to Eastern's schedule of classes for a term.
 * Course data is cached locally when available.
 */
public class EasternSchedule {

    private static final String BASE = "https://reg-prod.ec.easternct.edu/StudentRegistrationSsb";
    private String term;

    private CookieManager cookieManager;
    private HttpClient client;


    /**
     * Creates a schedule client for the default term.
     */
    public EasternSchedule()  {

        this.term = "202710";

        cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        
        client = HttpClient.newBuilder()
        .cookieHandler(cookieManager)
        .build();

    }

    /**
     * Accessor for term code
     * @return The term code
     */
    public String getTerm() {
        return term;
    }

    /**
     * Retrieves up to 25 terms from the cache or Eastern's schedule.
     *
     * @return the available terms as JSON
     * @throws IOException if cache file exists but cannot be read
     * @throws InterruptedException if a web request is interrupted
     */
    private String retrieveTerms() throws IOException, InterruptedException {
        Path cacheFile = Path.of("cache/terms.txt");
        String content;

        if (Files.exists(cacheFile)) {
            System.out.println("Using cached term data from " + cacheFile + "...");
            content = Files.readString(cacheFile);
            return content;
        }

        System.out.println("Retrieving data from URL ...");        
        String url = BASE + "/ssb/classSearch/getTerms?offset=1&max=25&searchTerm=";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected response status: " + response.statusCode());
        }

        content = response.body();
        Files.writeString(cacheFile, content);

        return content; 
    }

    /**
     * Parses the JSON content into terms.
     *
     * @param content JSON containing the available terms
     * @return the parsed terms
     * @throws IOException if terms cannot be parsed
     */
    private ArrayList<Term> parseTerms(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<Term> terms = mapper.readValue(
            content,
            new TypeReference<ArrayList<Term>>() {}            
        );

        return terms;
    }

    /** 
     * Parses content into a list of courses
     * @param content the content to parse
     * @return ArrayList<Course> an {@link ArrayList} of {@link Course} objects
     * @throws IOException if the JSON cannot be parsed
     */
    private ArrayList<Course> parseCourses(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode dataNode = mapper.readTree(content).get("data");        
        ArrayList<Course> courses = mapper.convertValue(dataNode, new TypeReference<ArrayList<Course>>() {});
        return courses;
    }

    /**
     * Retrieves and parses the available terms.
     *
     * @return the available terms
     * @throws IOException if cannot parse terms
     * @throws InterruptedException if a web request is interrupted
     */
    public ArrayList<Term>getAvailableTerms() throws IOException, InterruptedException {

        Files.createDirectories(Path.of("cache"));
        String content = retrieveTerms();
        ArrayList<Term> terms = parseTerms(content);
        return terms;
    }

    /**
     * Sets the active term on Eastern's schedule service.
     *
     * @return the server response body
     * @throws Exception if the request fails
     */

    private String postTerm() throws Exception {

        String url = BASE + "/ssb/term/search?mode=search";

        String body =
            "term=" + URLEncoder.encode(term, StandardCharsets.UTF_8) +
            "&studyPath=" +
            "&studyPathText=" +
            "&startDatepicker=" +
            "&endDatepicker=";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8"
            )            
            .POST(HttpRequest.BodyPublishers.ofString(body))            
            .build();

        HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                "setTerm failed: " + response.statusCode()
            );
        }

        return response.body();
}

    /**
     * Returns the cache file for a subject in the active term.
     *
     * @param subject the subject abbreviation
     * @return the cache file path
     */
    private Path createCacheFile(String subject) {
        return Path.of("cache/term_" + term + "_" + subject + ".txt");
    }

    /**
     * Caches courses for the active term and subject if no cache exists.
     *
     * @param subject the subject to cache courses for
     * @throws Exception if the service request or cache write fails
     */
    private void cacheCourses(String subject) throws Exception {
        
        Path cacheFile = createCacheFile(subject);

        if (Files.exists(cacheFile)) {            
           System.out.println("Using cached term data from " + cacheFile + "...");
           return;           
        }

        postTerm();

        String url = BASE + "/ssb/searchResults/searchResults?" +
        "txt_subject=" + subject + "&" +
        "txt_term=" + term + "&" +
        "startDatepicker=&endDatepicker=&pageOffset=0&pageMaxSize=500&sortColumn=subjectDescription&sortDirection=asc";
                                                    
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header(
                "Accept", "application/json"                    
            )              
            .GET()           
            .build();

        HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                "searchClasses failed: " + response.statusCode() + "\n" + response.body()
            );
        }

        Files.writeString(cacheFile, response.body());
    }

    /** 
     * Returns courses for a subject, using the local cache when available.
     *
     * @param subject the subject of courses to retrieve
     * @return an {@link ArrayList} of {@link Course} objects
     * @throws Exception if the schedule data cannot be retrieved or parsed
     */
    public ArrayList<Course> queryCourseBySubject(String subject) throws Exception {
        if (subject == null || !subject.matches("[A-Za-z]+")) {
            throw new IllegalArgumentException("Subject must contain letters only.");
        }

        String normalizedSubject = subject.toUpperCase(java.util.Locale.ROOT);
        Files.createDirectories(Path.of("cache"));
        cacheCourses(normalizedSubject);
        Path cacheFile = createCacheFile(normalizedSubject);
        String content = Files.readString(cacheFile);
        
        ArrayList<Course> courses = parseCourses(content);
        return courses;
    }   
}

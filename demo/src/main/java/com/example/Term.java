package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A term offered in Eastern's course schedule.
 *
 * @param code the term code
 * @param description the term name and status
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Term(String code, String description) {}

package com.foosball.dto;

/** Body for {@code PUT /players/{name}/rename}. */
public record RenameRequest(String newName) {
}

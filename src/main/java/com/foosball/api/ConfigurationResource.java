package com.foosball.api;

import com.foosball.domain.ConfigurationItem;
import com.foosball.dto.ConfigurationItemDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * {@code GET /configuration} — returns a JSON array of {@code {name, value}}
 * pairs (the table-name keys and number-of-tables setting).
 */
@Path("/configuration")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigurationResource {

    @GET
    public List<ConfigurationItemDto> listAll() {
        return ConfigurationItem.<ConfigurationItem>listAll()
                .stream()
                .map(c -> new ConfigurationItemDto(c.name, c.value))
                .toList();
    }
}

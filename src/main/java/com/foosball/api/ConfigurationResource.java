package com.foosball.api;

import com.foosball.domain.ConfigurationItem;
import com.foosball.dto.ConfigurationItemDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Configuration resource — port of legacy {@code Configuration.groovy}.
 *
 * <p>Wire contract preserved: returns a JSON array of {@code {name, value}}
 * pairs. The frontend reduces it to a map client-side and reads
 * {@code numberOfTables} / {@code nameTable<N>} keys.
 *
 * <p>Dropped vs. legacy: the per-request
 * {@code MoreUtil.ensureConfigurationTableExist()} DDL call —
 * Flyway V1 owns schema creation now.
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

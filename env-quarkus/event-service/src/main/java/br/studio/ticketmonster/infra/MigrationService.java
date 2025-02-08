package br.studio.ticketmonster.infra;

import org.flywaydb.core.Flyway;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MigrationService {
    // You can Inject the object if you want to use it manually
    @Inject
    Flyway flyway; 

    public void checkMigration() {
        // This will print 1.0.0
        System.out.println(flyway.info().current().getVersion().toString());
    }
}
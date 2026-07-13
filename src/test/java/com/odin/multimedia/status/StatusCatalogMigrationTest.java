package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class StatusCatalogMigrationTest {
    @Test void createsSchemaAndEnforcesCardinalityAndUniqueness() throws Exception {
        // H2 has no MariaDB compatibility mode recognized by Flyway 8; MySQL mode
        // exercises this portable DDL, while real MariaDB validation remains a Phase 2 prerequisite.
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load();
        assertEquals(2, flyway.migrate().migrationsExecuted); flyway.validate();
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            assertTrue(hasTable(c)); assertTrue(hasIndex(c, "IDX_STATUS_CATALOG_STATE_EXPIRES"));
            assertTrue(hasIndex(c, "IDX_STATUS_CATALOG_FEED_SEEK"));
            insert(c, "id1", "89", "key1", "legacy1");
            insert(c, "id2", "89", "key2", "legacy2");
            assertThrows(SQLException.class, () -> insert(c, "id3", "89", "key1", "legacy3"));
            assertThrows(SQLException.class, () -> insert(c, "id4", "90", "key4", "legacy1"));
        }
    }
    private void insert(Connection c, String id, String uploader, String key, String legacy) throws SQLException {
        String sql = "INSERT INTO status_catalog(status_id,uploader_id,legacy_status_key,media_type,created_at," +
                "expires_at,lifecycle_state,reconciliation_state,idempotency_key,idempotency_provenance," +
                "created_timestamp,updated_timestamp,version) VALUES(?,?,?,'IMAGE',CURRENT_TIMESTAMP," +
                "DATEADD('HOUR',24,CURRENT_TIMESTAMP),'UPLOADING','NONE',?,'CLIENT',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)";
        try (PreparedStatement ps = c.prepareStatement(sql)) { ps.setString(1,id); ps.setString(2,uploader); ps.setString(3,legacy); ps.setString(4,key); ps.executeUpdate(); }
    }
    private boolean hasTable(Connection c) throws SQLException {
        try (ResultSet r = c.getMetaData().getTables(null,null,"STATUS_CATALOG",new String[]{"TABLE"})) { return r.next(); }
    }
    private boolean hasIndex(Connection c, String expectedName) throws SQLException {
        try (ResultSet r = c.getMetaData().getIndexInfo(null,null,"STATUS_CATALOG",false,false)) {
            while (r.next()) if (expectedName.equalsIgnoreCase(r.getString("INDEX_NAME"))) return true;
            return false;
        }
    }
}

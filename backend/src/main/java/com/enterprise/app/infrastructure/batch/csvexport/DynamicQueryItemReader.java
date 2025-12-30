package com.enterprise.app.infrastructure.batch.csvexport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

/**
 * ItemReader qui exécute une requête SQL générée dynamiquement par PostgreSQL.
 * Utilise la fonction build_export_query() pour construire la requête d'export.
 */
@Slf4j
@Component
@StepScope
public final class DynamicQueryItemReader extends JdbcCursorItemReader<Map<String, Object>> {

    private static final int FETCH_SIZE = 1_000;

    private final DataSource dataSource;

    @Value("#{jobParameters['nomCsv']}")
    private String nomCsv;

    @Value("#{jobParameters['dateDebut'] ?: T(java.time.LocalDate).now().minusDays(7)}")
    private LocalDate dateDebut;

    @Value("#{jobParameters['dateFin'] ?: T(java.time.LocalDate).now()}")
    private LocalDate dateFin;

    public DynamicQueryItemReader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("""
                Initialisation DynamicQueryItemReader
                CSV     : {}
                Période : {} → {}
                """, nomCsv, dateDebut, dateFin);

        setDataSource(dataSource);
        setSql(loadQuery());
        setRowMapper(new ColumnMapRowMapper());
        setFetchSize(FETCH_SIZE);
        setPreparedStatementSetter(this::bindDates);

        super.afterPropertiesSet();
    }

    private void bindDates(PreparedStatement ps) throws SQLException {
        ps.setDate(1, Date.valueOf(dateDebut));
        ps.setDate(2, Date.valueOf(dateFin));
    }

    private String loadQuery() {
        if (!hasText(nomCsv)) {
            throw new IllegalArgumentException("nomCsv est obligatoire");
        }
        return fetchQueryFromDatabase();
    }

    private String fetchQueryFromDatabase() {
        log.debug("Chargement requête SQL pour {}", nomCsv);

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareCall("SELECT export_csv.build_export_query(?)")) {

            statement.setString(1, nomCsv);
            statement.execute();

            try (var rs = statement.getResultSet()) {
                if (rs.next()) {
                    var query = rs.getString(1);
                    log.info("Requête générée ({} caractères)", query.length());
                    return query;
                }
            }

            throw new IllegalStateException("Aucune requête générée pour le CSV : " + nomCsv);

        } catch (SQLException e) {
            throw new IllegalStateException("Erreur génération requête SQL pour " + nomCsv, e);
        }
    }
}


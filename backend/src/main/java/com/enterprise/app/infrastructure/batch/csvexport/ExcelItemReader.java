package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.application.dto.csvexport.ExcelRowDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * ItemReader qui lit un fichier Excel multi-feuilles et préserve l'ordre des lignes.
 * Chaque feuille correspond à un CSV (CSV_1, CSV_2, CSV_3).
 *
 * IMPORTANT: L'ordre des lignes Excel détermine l'ordre des colonnes CSV.
 */
@Slf4j
@Component
@StepScope
public final class ExcelItemReader
        extends AbstractItemStreamItemReader<ExcelRowDTO> {

    private static final int BASE_COLUMN_COUNT = 5;

    @Value("#{jobParameters['excelFilePath']}")
    private String excelFilePath;

    private Workbook workbook;
    private FormulaEvaluator evaluator;

    private Iterator<Sheet> sheets;
    private Iterator<Row> rows;

    private String currentSheet;
    private int totalRead;

    @Override
    public void open(ExecutionContext ctx) {
        try {
            workbook = new XSSFWorkbook(new FileInputStream(excelFilePath));
            evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            sheets = workbook.iterator();

            advanceSheet();

            log.info("Excel ouvert: {}", excelFilePath);

        } catch (IOException e) {
            throw new ItemStreamException("Impossible d’ouvrir Excel", e);
        }
    }

    @Override
    public ExcelRowDTO read() {
        while (true) {

            if (rows == null || !rows.hasNext()) {
                if (!advanceSheet()) {
                    log.info("Lecture Excel terminée: {} lignes", totalRead);
                    return null;
                }
            }

            var row = rows.next();
            if (isEmpty(row)) continue;

            var dto = map(row);
            totalRead++;

            if (dto.isValid()) return dto;

            log.warn("Ligne invalide ignorée: feuille={}, ligne={}",
                    currentSheet, row.getRowNum());
        }
    }

    private boolean advanceSheet() {
        if (!sheets.hasNext()) return false;

        var sheet = sheets.next();
        currentSheet = sheet.getSheetName();
        rows = sheet.iterator();

        if (rows.hasNext()) rows.next(); // header

        log.info("Feuille: {} ({} lignes)", currentSheet, sheet.getLastRowNum());
        return true;
    }

    private ExcelRowDTO map(Row row) {
        var valeursPossibles = new java.util.HashMap<Integer, String>();

        for (int col = BASE_COLUMN_COUNT, pos = 1;
             col < row.getLastCellNum();
             col++) {

            var val = value(row, col);
            if (val != null && !val.isBlank()) {
                valeursPossibles.put(pos++, val);
            }
        }

        return ExcelRowDTO.create(
                currentSheet,
                row.getRowNum(),
                value(row, 0),
                value(row, 1),
                value(row, 2),
                value(row, 3),
                value(row, 4),
                valeursPossibles
        );
    }

    private boolean isEmpty(Row row) {
        return IntStream.range(0, BASE_COLUMN_COUNT)
                .mapToObj(i -> value(row, i))
                .allMatch(v -> v == null || v.isBlank());
    }

    private String value(Row row, int col) {
        var cell = row.getCell(col);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case NUMERIC -> formatNumeric(cell);
            case FORMULA -> evaluate(cell);
            default -> null;
        };
    }

    private String formatNumeric(Cell cell) {
        var n = cell.getNumericCellValue();
        return n == Math.floor(n)
                ? String.valueOf((long) n)
                : String.valueOf(n);
    }

    private String evaluate(Cell cell) {
        try {
            var v = evaluator.evaluate(cell);
            return switch (v.getCellType()) {
                case STRING -> v.getStringValue().trim();
                case BOOLEAN -> String.valueOf(v.getBooleanValue());
                case NUMERIC -> String.valueOf(v.getNumberValue());
                default -> null;
            };
        } catch (Exception e) {
            log.warn("Erreur formule ligne {}", cell.getRowIndex(), e);
            return null;
        }
    }

    @Override
    public void update(ExecutionContext ctx) {
        ctx.putInt("excel.totalRead", totalRead);
        ctx.putString("excel.currentSheet", currentSheet);
    }

    @Override
    public void close() {
        try {
            if (workbook != null) workbook.close();
        } catch (IOException e) {
            throw new ItemStreamException("Erreur fermeture Excel", e);
        }
    }
}

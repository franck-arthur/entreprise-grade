package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.application.dto.csvexport.ExcelRowDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.ExecutionContext;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExcelItemReader - Lecteur de fichiers Excel")
class ExcelItemReaderTest {

    private ExcelItemReader reader;
    private ExecutionContext executionContext;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        reader = new ExcelItemReader();
        executionContext = new ExecutionContext();
    }

    @Test
    @DisplayName("Devrait lire un fichier Excel avec succès")
    void shouldReadExcelFileSuccessfully() throws Exception {
        // Given
        String excelFilePath = createTestExcelFile();
        setPrivateField(reader, "excelFilePath", excelFilePath);

        reader.open(executionContext);

        // When
        ExcelRowDTO row1 = reader.read();
        ExcelRowDTO row2 = reader.read();
        ExcelRowDTO row3 = reader.read(); // Doit être null (fin)

        // Then
        assertThat(row1).isNotNull();
        assertThat(row1.nomCsv()).isEqualTo("CSV_1");
        assertThat(row1.codeVariable()).isEqualTo("COL1");
        assertThat(row1.libelle()).isEqualTo("Colonne 1");

        assertThat(row2).isNotNull();
        assertThat(row2.nomCsv()).isEqualTo("CSV_1");
        assertThat(row2.codeVariable()).isEqualTo("COL2");
        assertThat(row2.libelle()).isEqualTo("Colonne 2");

        assertThat(row3).isNull(); // Fin de fichier

        reader.close();
    }

    @Test
    @DisplayName("Devrait ignorer les lignes vides")
    void shouldIgnoreEmptyRows() throws Exception {
        // Given
        String excelFilePath = createTestExcelFileWithEmptyRows();
        setPrivateField(reader, "excelFilePath", excelFilePath);

        reader.open(executionContext);

        // When
        ExcelRowDTO row1 = reader.read();
        ExcelRowDTO row2 = reader.read(); // La ligne vide doit être ignorée
        ExcelRowDTO row3 = reader.read(); // Doit être null

        // Then
        assertThat(row1).isNotNull();
        assertThat(row1.codeVariable()).isEqualTo("COL1");

        assertThat(row2).isNotNull();
        assertThat(row2.codeVariable()).isEqualTo("COL3"); // COL2 était vide, donc ignorée

        assertThat(row3).isNull();

        reader.close();
    }

    @Test
    @DisplayName("Devrait traiter plusieurs feuilles Excel")
    void shouldProcessMultipleSheets() throws Exception {
        // Given
        String excelFilePath = createMultiSheetExcelFile();
        setPrivateField(reader, "excelFilePath", excelFilePath);

        reader.open(executionContext);

        // When
        ExcelRowDTO row1 = reader.read(); // Première feuille
        ExcelRowDTO row2 = reader.read(); // Deuxième feuille
        ExcelRowDTO row3 = reader.read(); // Fin

        // Then
        assertThat(row1).isNotNull();
        assertThat(row1.nomCsv()).isEqualTo("CSV_1");

        assertThat(row2).isNotNull();
        assertThat(row2.nomCsv()).isEqualTo("CSV_2");

        assertThat(row3).isNull();

        reader.close();
    }

    @Test
    @DisplayName("Devrait gérer les cellules avec différents types")
    void shouldHandleDifferentCellTypes() throws Exception {
        // Given
        String excelFilePath = createExcelFileWithDifferentTypes();
        setPrivateField(reader, "excelFilePath", excelFilePath);

        reader.open(executionContext);

        // When
        ExcelRowDTO row = reader.read();

        // Then
        assertThat(row).isNotNull();
        assertThat(row.codeVariable()).isNotNull();
        assertThat(row.libelle()).isNotNull();

        reader.close();
    }

    @Test
    @DisplayName("Devrait lancer une exception pour un fichier inexistant")
    void shouldThrowExceptionForNonExistentFile() {
        // Given
        String nonExistentPath = "/nonexistent/file.xlsx";
        setPrivateField(reader, "excelFilePath", nonExistentPath);

        // When & Then
        assertThatThrownBy(() -> reader.open(executionContext))
                .hasMessageContaining("Impossible d'ouvrir Excel");
    }

    @Test
    @DisplayName("Devrait préserver l'ordre des lignes Excel")
    void shouldPreserveExcelRowOrder() throws Exception {
        // Given
        String excelFilePath = createTestExcelFile();
        setPrivateField(reader, "excelFilePath", excelFilePath);

        reader.open(executionContext);

        // When
        ExcelRowDTO row1 = reader.read();
        ExcelRowDTO row2 = reader.read();

        // Then
        assertThat(row1.excelRowNumber()).isLessThan(row2.excelRowNumber());

        reader.close();
    }

    @Test
    @DisplayName("Devrait mettre à jour le contexte d'exécution")
    void shouldUpdateExecutionContext() throws Exception {
        // Given
        String excelFilePath = createTestExcelFile();
        setPrivateField(reader, "excelFilePath", excelFilePath);

        reader.open(executionContext);

        // When
        reader.read();
        reader.update(executionContext);

        // Then
        assertThat(executionContext.getInt("excel.totalRowsRead", 0)).isGreaterThan(0);

        reader.close();
    }

    private String createTestExcelFile() throws IOException {
        String fileName = tempDir.resolve("test.xlsx").toString();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CSV_1");

            // En-tête - 5 colonnes de base
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Code Variable");
            headerRow.createCell(1).setCellValue("Libellé");
            headerRow.createCell(2).setCellValue("Format");
            headerRow.createCell(3).setCellValue("Type Question");
            headerRow.createCell(4).setCellValue("Règle Gestion");

            // Données
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("COL1");
            row1.createCell(1).setCellValue("Colonne 1");
            row1.createCell(2).setCellValue("TEXT");
            row1.createCell(3).setCellValue("TEXTE");
            row1.createCell(4).setCellValue("TEXTE_DIRECT");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("COL2");
            row2.createCell(1).setCellValue("Colonne 2");
            row2.createCell(2).setCellValue("NUMBER");
            row2.createCell(3).setCellValue("NUMERIQUE");
            row2.createCell(4).setCellValue("NUMERIQUE_DIRECT");

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                workbook.write(fos);
            }
        }

        return fileName;
    }

    private String createTestExcelFileWithEmptyRows() throws IOException {
        String fileName = tempDir.resolve("test-empty.xlsx").toString();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CSV_1");

            // En-tête - 5 colonnes de base
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Code Variable");
            headerRow.createCell(1).setCellValue("Libellé");
            headerRow.createCell(2).setCellValue("Format");
            headerRow.createCell(3).setCellValue("Type Question");
            headerRow.createCell(4).setCellValue("Règle Gestion");

            // Données avec ligne vide
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("COL1");
            row1.createCell(1).setCellValue("Colonne 1");
            row1.createCell(2).setCellValue("TEXT");
            row1.createCell(3).setCellValue("TEXTE");
            row1.createCell(4).setCellValue("TEXTE_DIRECT");

            // Ligne 2 vide (sera ignorée)
            sheet.createRow(2);

            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("COL3");
            row3.createCell(1).setCellValue("Colonne 3");
            row3.createCell(2).setCellValue("TEXT");
            row3.createCell(3).setCellValue("TEXTE");
            row3.createCell(4).setCellValue("TEXTE_DIRECT");

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                workbook.write(fos);
            }
        }

        return fileName;
    }

    private String createMultiSheetExcelFile() throws IOException {
        String fileName = tempDir.resolve("test-multi.xlsx").toString();

        try (Workbook workbook = new XSSFWorkbook()) {
            // Première feuille
            Sheet sheet1 = workbook.createSheet("CSV_1");
            Row headerRow1 = sheet1.createRow(0);
            headerRow1.createCell(0).setCellValue("Code Variable");
            headerRow1.createCell(1).setCellValue("Libellé");
            headerRow1.createCell(2).setCellValue("Format");
            headerRow1.createCell(3).setCellValue("Type Question");
            headerRow1.createCell(4).setCellValue("Règle Gestion");
            Row row1 = sheet1.createRow(1);
            row1.createCell(0).setCellValue("COL1");
            row1.createCell(1).setCellValue("Colonne 1");
            row1.createCell(2).setCellValue("TEXT");
            row1.createCell(3).setCellValue("TEXTE");
            row1.createCell(4).setCellValue("TEXTE_DIRECT");

            // Deuxième feuille
            Sheet sheet2 = workbook.createSheet("CSV_2");
            Row headerRow2 = sheet2.createRow(0);
            headerRow2.createCell(0).setCellValue("Code Variable");
            headerRow2.createCell(1).setCellValue("Libellé");
            headerRow2.createCell(2).setCellValue("Format");
            headerRow2.createCell(3).setCellValue("Type Question");
            headerRow2.createCell(4).setCellValue("Règle Gestion");
            Row row2 = sheet2.createRow(1);
            row2.createCell(0).setCellValue("COL2");
            row2.createCell(1).setCellValue("Colonne 2");
            row2.createCell(2).setCellValue("NUMBER");
            row2.createCell(3).setCellValue("NUMERIQUE");
            row2.createCell(4).setCellValue("NUMERIQUE_DIRECT");

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                workbook.write(fos);
            }
        }

        return fileName;
    }

    private String createExcelFileWithDifferentTypes() throws IOException {
        String fileName = tempDir.resolve("test-types.xlsx").toString();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CSV_1");

            // En-tête - 5 colonnes de base
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Code Variable");
            headerRow.createCell(1).setCellValue("Libellé");
            headerRow.createCell(2).setCellValue("Format");
            headerRow.createCell(3).setCellValue("Type Question");
            headerRow.createCell(4).setCellValue("Règle Gestion");

            // Données avec différents types
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("COL1"); // String
            row1.createCell(1).setCellValue(123.45); // Number comme libellé
            row1.createCell(2).setCellValue(true);   // Boolean comme format
            row1.createCell(3).setCellValue("MIXTE");
            row1.createCell(4).setCellValue("TEXTE_DIRECT");

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                workbook.write(fos);
            }
        }

        return fileName;
    }

    private void setPrivateField(Object object, String fieldName, Object value) {
        try {
            var field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de définir le champ " + fieldName, e);
        }
    }
}
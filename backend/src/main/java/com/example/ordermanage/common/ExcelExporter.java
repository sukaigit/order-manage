package com.example.ordermanage.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public final class ExcelExporter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ExcelExporter() {
    }

    public static byte[] export(List<String> headers, List<List<String>> rows) {
        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("导出");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }
            int rowIndex = 1;
            for (List<String> values : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < values.size(); i++) {
                    String value = values.get(i);
                    row.createCell(i).setCellValue(value == null ? "" : value);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("导出文件生成失败", e);
        }
    }

    public static String formatTime(LocalDateTime time) {
        return time == null ? "" : TIME_FORMAT.format(time);
    }
}

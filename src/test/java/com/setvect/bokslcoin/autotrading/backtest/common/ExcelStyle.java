package com.setvect.bokslcoin.autotrading.backtest.common;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

public class ExcelStyle {
    public static XSSFCellStyle createDefault(XSSFWorkbook workbook) {
        return workbook.createCellStyle();
    }

    public static XSSFCellStyle createDate(XSSFWorkbook workbook) {
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy/MM/dd"));
        return cellStyle;
    }

    public static XSSFCellStyle createDateTime(XSSFWorkbook workbook) {
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy/MM/dd HH:mm"));
        return cellStyle;
    }

    public static XSSFCellStyle createYearMonth(XSSFWorkbook workbook) {
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy/MM"));
        return cellStyle;
    }

    public static XSSFCellStyle createComma(XSSFWorkbook workbook) {
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        cellStyle.setDataFormat(format.getFormat("###,###"));
        return cellStyle;
    }

    /**
     * ¼Ò¼öÁ¡ Ç¥½Ã
     */
    public static XSSFCellStyle createDecimal(XSSFWorkbook workbook) {
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        cellStyle.setDataFormat(format.getFormat("0.00"));
        return cellStyle;
    }

    public static XSSFCellStyle createCommaDecimal(XSSFWorkbook workbook) {
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        cellStyle.setDataFormat(format.getFormat("###,###.00"));
        return cellStyle;
    }

    public static XSSFCellStyle createPercent(XSSFWorkbook workbook) {
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        cellStyle.setDataFormat(format.getFormat("###,##0.00%"));
        return cellStyle;
    }

    public static XSSFCellStyle createHeaderRow(XSSFWorkbook workbook) {
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFillForegroundColor(IndexedColors.YELLOW.index);

        XSSFFont font = workbook.createFont();
        font.setBold(true);
        cellStyle.setFont(font);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return cellStyle;
    }

    /**
     * ¸ðµç ¼¿ border Àû¿ë
     */
    public static void applyAllBorder(XSSFSheet sheet) {
        int rowCount = sheet.getPhysicalNumberOfRows();
        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
            XSSFRow row = sheet.getRow(rowIdx);
            int cellCount = row.getPhysicalNumberOfCells();
            for (int cellIdx = 0; cellIdx < cellCount; cellIdx++) {
                XSSFCell cell = row.getCell(cellIdx);
                XSSFCellStyle cellStyle = cell.getCellStyle();
                cellStyle.setBorderLeft(BorderStyle.THIN);
                cellStyle.setBorderLeft(BorderStyle.THIN);
                cellStyle.setBorderLeft(BorderStyle.THIN);
                cellStyle.setBorderLeft(BorderStyle.THIN);
            }
        }
    }

    public static void applyDefaultFont(XSSFSheet sheet) {
        int rowCount = sheet.getPhysicalNumberOfRows();
        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
            XSSFRow row = sheet.getRow(rowIdx);
            int cellCount = row.getPhysicalNumberOfCells();
            for (int cellIdx = 0; cellIdx < cellCount; cellIdx++) {
                XSSFCellStyle cellStyle = row.getCell(cellIdx).getCellStyle();
                cellStyle.getFont().setFontName("¸¼Àº °íµñ");
            }
        }
    }
}

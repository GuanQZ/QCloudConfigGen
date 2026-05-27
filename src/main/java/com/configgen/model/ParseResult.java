package com.configgen.model;

import java.util.List;
import java.util.Map;

public class ParseResult {
    private List<String> columns;
    private List<Map<String, String>> rows;
    private int totalRows;
    private String filename;

    public ParseResult() {
    }

    public ParseResult(List<String> columns, List<Map<String, String>> rows, int totalRows, String filename) {
        this.columns = columns;
        this.rows = rows;
        this.totalRows = totalRows;
        this.filename = filename;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<Map<String, String>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, String>> rows) {
        this.rows = rows;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
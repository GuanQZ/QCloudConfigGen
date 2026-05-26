package com.configgen.model;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParseResult {
    private List<String> columns;
    private List<Map<String, String>> rows;
    private int totalRows;
    private String filename;
}
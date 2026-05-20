package com.vector.onboarding.domain.dataview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SchemaParserService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static class ParsedTable {
        String name;
        List<Map<String, String>> columns = new ArrayList<>();
        Set<String> parents = new HashSet<>();
        Set<String> children = new HashSet<>();
    }

    public String parseSchema(String fileContents) {
        log.info("자바 정규식(Regex)을 활용하여 스키마 파일을 분석하고 레이아웃을 계산합니다 (LLM 미사용).");
        
        Map<String, ParsedTable> tables = new LinkedHashMap<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> uniqueRelations = new HashSet<>();

        // ============================================
        // 1. Prisma 스키마 파싱 로직
        // ============================================
        Pattern modelPattern = Pattern.compile("model\\s+(\\w+)\\s+\\{([^}]+)\\}", Pattern.MULTILINE);
        Matcher modelMatcher = modelPattern.matcher(fileContents);
        
        while (modelMatcher.find()) {
            String modelName = modelMatcher.group(1);
            String body = modelMatcher.group(2);
            ParsedTable table = new ParsedTable();
            table.name = modelName;
            
            String[] lines = body.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("//") || line.startsWith("@@")) continue;
                
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String colName = parts[0];
                    String colType = parts[1];
                    
                    Map<String, String> colMap = new HashMap<>();
                    colMap.put("name", colName);
                    
                    if (line.contains("@id")) {
                        colMap.put("type", colType + " (PK)");
                    } else if (line.contains("@unique")) {
                        colMap.put("type", colType + " (Unique)");
                    } else {
                        colMap.put("type", colType);
                    }
                    table.columns.add(colMap);
                    
                    // 관계 정보 1차 수집
                    if (line.contains("@relation")) {
                        colMap.put("type", colType + " (FK)");
                        table.parents.add(colType.toLowerCase());
                    } else if (colType.endsWith("[]")) {
                        String targetModel = colType.replace("[]", "").toLowerCase();
                        table.children.add(targetModel);
                    }
                }
            }
            tables.put(modelName.toLowerCase(), table);
        }

        // ============================================
        // 2. SQL CREATE TABLE 파싱 로직
        // ============================================
        Pattern sqlPattern = Pattern.compile("CREATE\\s+TABLE\\s+(\\w+)\\s+\\(([^;]+)\\);", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher sqlMatcher = sqlPattern.matcher(fileContents);
        
        while (sqlMatcher.find()) {
            String tableName = sqlMatcher.group(1);
            String body = sqlMatcher.group(2);
            ParsedTable table = new ParsedTable();
            table.name = tableName;
            
            String[] lines = body.split(",");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.toUpperCase().startsWith("PRIMARY KEY") || line.toUpperCase().startsWith("FOREIGN KEY")) continue;
                
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    Map<String, String> colMap = new HashMap<>();
                    colMap.put("name", parts[0]);
                    String type = parts[1];
                    if (line.toUpperCase().contains("PRIMARY KEY")) type += " (PK)";
                    colMap.put("type", type);
                    table.columns.add(colMap);
                }
            }
            tables.put(tableName.toLowerCase(), table);
        }

        // ============================================
        // 3. 관계 무결성 보완 및 SQL FK 유추
        // ============================================
        // Prisma 양방향 매핑 대칭화
        for (ParsedTable table : tables.values()) {
            String name = table.name.toLowerCase();
            for (String parent : new ArrayList<>(table.parents)) {
                if (tables.containsKey(parent)) {
                    tables.get(parent).children.add(name);
                }
            }
            for (String child : new ArrayList<>(table.children)) {
                if (tables.containsKey(child)) {
                    tables.get(child).parents.add(name);
                }
            }
        }
        
        // SQL 네이밍 컨벤션을 활용한 FK 유추 (예: Payment의 order_id -> Order 테이블 참조)
        for (ParsedTable table : tables.values()) {
            String name = table.name.toLowerCase();
            for (Map<String, String> col : table.columns) {
                String colName = col.get("name").toLowerCase();
                for (String otherName : tables.keySet()) {
                    if (name.equals(otherName)) continue;
                    if (colName.equals(otherName + "id") || colName.equals(otherName + "_id")) {
                        table.parents.add(otherName);
                        tables.get(otherName).children.add(name);
                        if (!col.get("type").contains("(FK)")) {
                            col.put("type", col.get("type") + " (FK)");
                        }
                    }
                }
            }
        }

        // ============================================
        // 4. 레이아웃 엔진 (트리 구조 배치)
        // ============================================
        Map<String, int[]> positions = new HashMap<>();
        Set<String> placed = new HashSet<>();
        
        // 상위 테이블(Root) 찾기: 부모 테이블이 없는 테이블
        List<String> roots = new ArrayList<>();
        for (String name : tables.keySet()) {
            ParsedTable table = tables.get(name);
            boolean hasParentInTables = false;
            for (String p : table.parents) {
                if (tables.containsKey(p)) {
                    hasParentInTables = true;
                    break;
                }
            }
            if (!hasParentInTables) {
                roots.add(name);
            }
        }
        
        if (roots.isEmpty() && !tables.isEmpty()) {
            roots.add(tables.keySet().iterator().next());
        }

        // 루트별로 우측으로 이동하며 트리 배치
        int currentX = 100;
        for (String root : roots) {
            if (placed.contains(root)) continue;
            int nextX = layoutTree(root, currentX, 100, positions, placed, tables);
            currentX = nextX + 380; // 테이블 폭 288px + 여백
        }

        // 아직 배치되지 않은 미연결 테이블 배치 (안전을 위한 폴백)
        for (String name : tables.keySet()) {
            if (!placed.contains(name)) {
                int nextX = layoutTree(name, currentX, 100, positions, placed, tables);
                currentX = nextX + 380;
            }
        }

        // ============================================
        // 5. 노드 및 엣지 JSON 데이터 생성
        // ============================================
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (String name : tables.keySet()) {
            ParsedTable table = tables.get(name);
            int[] pos = positions.getOrDefault(name, new int[]{100, 100});
            nodes.add(createNode(table.name, table.columns, pos[0], pos[1]));

            // 부모 테이블과의 연결선(Edge) 생성
            for (String parentName : table.parents) {
                if (!tables.containsKey(parentName)) continue;
                String source = parentName;
                String target = name;
                String relKey1 = source + "-" + target;
                String relKey2 = target + "-" + source;

                if (!uniqueRelations.contains(relKey1) && !uniqueRelations.contains(relKey2)) {
                    uniqueRelations.add(relKey1);
                    
                    ParsedTable parentTable = tables.get(parentName);
                    String label = getEdgeLabel(parentTable, table);

                    Map<String, Object> edge = new HashMap<>();
                    edge.put("id", "e-" + source + "-" + target);
                    edge.put("source", source);
                    edge.put("target", target);
                    edge.put("sourceHandle", "bottom");
                    edge.put("targetHandle", "top");
                    edge.put("type", "customEdge");

                    Map<String, String> edgeData = new HashMap<>();
                    edgeData.put("label", label);
                    
                    // 스타일 구분 (FK는 회색, Array 형태의 1:N은 파란색)
                    if (label.contains("[]") || label.toLowerCase().contains("order")) {
                        edgeData.put("labelClassName", "bg-blue-50 text-blue-700 text-[10px] font-medium px-2 py-1 rounded border border-blue-200 shadow-sm z-10");
                    } else {
                        edgeData.put("labelClassName", "bg-gray-50 text-gray-700 text-[10px] font-medium px-2 py-1 rounded border border-gray-200 shadow-sm z-10");
                    }
                    
                    edge.put("data", edgeData);
                    edges.add(edge);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("JSON 파싱 변환 실패", e);
            return "{ \"nodes\": [], \"edges\": [] }";
        }
    }

    /**
     * 재귀적으로 자식 테이블을 아래쪽(y축 방향)으로 배치하고, 형제 노드가 있으면 우측(x축 방향)으로 펼침
     */
    private int layoutTree(String modelName, int x, int y, Map<String, int[]> positions, Set<String> placed, Map<String, ParsedTable> tables) {
        if (placed.contains(modelName)) return x;
        placed.add(modelName);
        positions.put(modelName, new int[]{x, y});
        
        ParsedTable table = tables.get(modelName);
        if (table == null) return x;
        
        List<String> children = new ArrayList<>();
        for (String child : table.children) {
            if (tables.containsKey(child) && !placed.contains(child)) {
                children.add(child);
            }
        }
        
        if (children.isEmpty()) {
            return x;
        }
        
        int childX = x;
        for (int i = 0; i < children.size(); i++) {
            String child = children.get(i);
            if (placed.contains(child)) continue;
            
            // 두 번째 자식부터는 우측으로 넓혀서 배치
            if (i > 0) {
                childX += 380;
            }
            // 자식은 320px 아래로 배치
            int nextX = layoutTree(child, childX, y + 320, positions, placed, tables);
            childX = Math.max(childX, nextX);
        }
        return childX;
    }

    /**
     * "상위 데이터베이스의 구성요소이름:하위 데이터베이스의 이름" 포맷으로 라벨을 추출
     */
    private String getEdgeLabel(ParsedTable parent, ParsedTable child) {
        String parentName = parent.name;
        String childName = child.name;

        // 1. Parent 테이블에 Child 테이블의 리스트 필드가 있는지 확인 (예: orders Order[])
        for (Map<String, String> col : parent.columns) {
            String colType = col.get("type");
            if (colType.replace("[]", "").equalsIgnoreCase(childName)) {
                return col.get("name") + ":" + childName;
            }
        }

        // 2. Child 테이블에 Parent 테이블을 가리키는 객체 필드가 있는지 확인 (예: user User)
        for (Map<String, String> col : child.columns) {
            String colType = col.get("type");
            if (colType.replace(" (FK)", "").equalsIgnoreCase(parentName)) {
                return col.get("name") + ":" + parentName;
            }
        }

        // 3. SQL FK 컬럼명 유추 (예: order_id -> order_id:Payment)
        for (Map<String, String> col : child.columns) {
            String colName = col.get("name").toLowerCase();
            if (colName.contains(parentName.toLowerCase())) {
                return col.get("name") + ":" + childName;
            }
        }

        // 기본값
        return parentName.toLowerCase() + ":" + childName;
    }

    private Map<String, Object> createNode(String name, List<Map<String, String>> columns, int x, int y) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", name.toLowerCase());
        node.put("type", "tableNode");
        
        Map<String, Integer> position = new HashMap<>();
        position.put("x", x);
        position.put("y", y);
        node.put("position", position);
        
        Map<String, Object> data = new HashMap<>();
        data.put("title", name);
        data.put("icon", "Database");
        data.put("columns", columns);
        node.put("data", data);
        return node;
    }
}

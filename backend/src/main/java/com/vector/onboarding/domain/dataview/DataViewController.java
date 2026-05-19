package com.vector.onboarding.domain.dataview;

import com.vector.onboarding.domain.dataview.service.DataViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/data-view")
@RequiredArgsConstructor
public class DataViewController {

    private final DataViewService dataViewService;

    @GetMapping(value = "/schema", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSchema(@RequestParam String repositoryUrl) {
        String schemaJson = dataViewService.getOrAnalyzeSchema(repositoryUrl);
        return ResponseEntity.ok(schemaJson);
    }
}

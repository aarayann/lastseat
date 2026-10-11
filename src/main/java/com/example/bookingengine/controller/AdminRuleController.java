package com.example.bookingengine.controller;

import java.util.List;

import com.example.bookingengine.dto.RuleConfigUpdate;
import com.example.bookingengine.dto.RuleConfigView;
import com.example.bookingengine.service.RuleConfigService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rules")
public class AdminRuleController {

    private final RuleConfigService ruleConfigService;

    public AdminRuleController(RuleConfigService ruleConfigService) {
        this.ruleConfigService = ruleConfigService;
    }

    @GetMapping
    public List<RuleConfigView> list(@RequestParam(name = "clientId", defaultValue = "acme") String clientId) {
        return ruleConfigService.listRules(clientId);
    }

    @PutMapping("/{id}")
    public RuleConfigView update(@PathVariable("id") Long id, @RequestBody RuleConfigUpdate update) {
        return ruleConfigService.update(id, update);
    }
}

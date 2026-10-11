package com.example.bookingengine.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.example.bookingengine.domain.ConfigVersion;
import com.example.bookingengine.domain.Decision;
import com.example.bookingengine.domain.RuleConfig;
import com.example.bookingengine.domain.RuleType;
import com.example.bookingengine.dto.RuleConfigUpdate;
import com.example.bookingengine.dto.RuleConfigView;
import com.example.bookingengine.engine.ActiveRule;
import com.example.bookingengine.engine.RuleEngine;
import com.example.bookingengine.engine.RuleParams;
import com.example.bookingengine.engine.RuleSet;
import com.example.bookingengine.exception.BadRequestException;
import com.example.bookingengine.exception.NotFoundException;
import com.example.bookingengine.repository.ConfigVersionRepository;
import com.example.bookingengine.repository.RuleConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleConfigService {

    private static final Logger log = LoggerFactory.getLogger(RuleConfigService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final RuleConfigRepository ruleConfigs;
    private final ConfigVersionRepository versions;
    private final RuleEngine engine;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RuleConfigService(RuleConfigRepository ruleConfigs, ConfigVersionRepository versions,
                             RuleEngine engine, ObjectMapper objectMapper, Clock clock) {
        this.ruleConfigs = ruleConfigs;
        this.versions = versions;
        this.engine = engine;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Cacheable("activeRules")
    @Transactional(readOnly = true)
    public RuleSet activeRules(String clientId, Long costCentreId) {
        Map<RuleType, RuleConfig> chosen = new EnumMap<>(RuleType.class);
        for (RuleConfig config : ruleConfigs.findByClientIdOrderByIdAsc(clientId)) {
            if (costCentreId.equals(config.getCostCentreId())) {
                chosen.put(config.getRuleType(), config);
            } else if (config.getCostCentreId() == null) {
                chosen.putIfAbsent(config.getRuleType(), config);
            }
        }

        List<ActiveRule> active = new ArrayList<>();
        for (RuleConfig config : chosen.values()) {
            if (config.isEnabled()) {
                active.add(new ActiveRule(config.getId(), config.getRuleType(), config.getPriority(),
                        config.getOnFail(), parse(config.getParams())));
            }
        }

        long version = currentVersion(clientId);
        log.info("Loaded {} active rules for client={} costCentre={} (config version {})",
                active.size(), clientId, costCentreId, version);
        return new RuleSet(version, RuleEngine.sortForEvaluation(active));
    }

    @Transactional(readOnly = true)
    public List<RuleConfigView> listRules(String clientId) {
        return ruleConfigs.findByClientIdOrderByIdAsc(clientId).stream()
                .map(this::toView)
                .toList();
    }

    @CacheEvict(value = "activeRules", allEntries = true)
    @Transactional
    public RuleConfigView update(Long id, RuleConfigUpdate update) {
        RuleConfig config = ruleConfigs.findById(id)
                .orElseThrow(() -> new NotFoundException("Rule", id));

        if (update.onFail() == Decision.ALLOW) {
            throw new BadRequestException("INVALID_RULE_CONFIG", "onFail must be DENY or NEEDS_APPROVAL");
        }
        if (update.params() != null) {
            engine.ruleFor(config.getRuleType()).validateParams(new RuleParams(update.params()));
            config.setParams(toJson(update.params()));
        }
        if (update.enabled() != null) {
            config.setEnabled(update.enabled());
        }
        if (update.priority() != null) {
            config.setPriority(update.priority());
        }
        if (update.onFail() != null) {
            config.setOnFail(update.onFail());
        }
        config.setUpdatedAt(LocalDateTime.now(clock));

        ConfigVersion version = versions.findById(config.getClientId())
                .orElseGet(() -> new ConfigVersion(config.getClientId(), 0));
        version.increment();
        versions.save(version);

        log.info("Rule {} ({}) updated; config version is now {}", id, config.getRuleType(), version.getVersion());
        return toView(config);
    }

    private long currentVersion(String clientId) {
        return versions.findById(clientId).map(ConfigVersion::getVersion).orElse(0L);
    }

    private RuleConfigView toView(RuleConfig config) {
        return new RuleConfigView(config.getId(), config.getClientId(), config.getCostCentreId(),
                config.getRuleType(), config.isEnabled(), config.getPriority(), config.getOnFail(),
                parse(config.getParams()).asMap());
    }

    private RuleParams parse(String json) {
        if (json == null || json.isBlank()) {
            return RuleParams.empty();
        }
        try {
            return new RuleParams(objectMapper.readValue(json, MAP_TYPE));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored rule params are not valid JSON: " + json, e);
        }
    }

    private String toJson(Map<String, Object> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("INVALID_RULE_PARAMS", "params could not be saved as JSON");
        }
    }
}

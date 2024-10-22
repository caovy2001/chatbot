package com.caovy2001.chatbot.service.script;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.*;
import com.caovy2001.chatbot.model.DateFilter;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.repository.ScriptRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.jedis.IJedisService;
import com.caovy2001.chatbot.service.jedis.JedisService;
import com.caovy2001.chatbot.service.node.INodeService;
import com.caovy2001.chatbot.service.script.command.CommandGetListScript;
import com.caovy2001.chatbot.service.script.command.CommandScriptAdd;
import com.caovy2001.chatbot.service.script.command.CommandScriptUpdate;
import com.caovy2001.chatbot.service.script.response.ResponseScript;
import com.caovy2001.chatbot.service.script.response.ResponseScriptAdd;
import com.caovy2001.chatbot.service.script.response.ResponseScriptGetByUserId;
import com.caovy2001.chatbot.service.script_intent_mapping.IScriptIntentMappingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.caovy2001.chatbot.service.jedis.JedisService.PrefixRedisKey.COLON;

@Service
@Slf4j
public class ScriptService extends BaseService implements IScriptService {
    @Autowired
    private ScriptRepository scriptRepository;

    @Autowired
    private INodeService nodeService;

    @Autowired
    private IScriptIntentMappingService scriptIntentMappingService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IJedisService jedisService;

    @Override
    public ResponseScriptAdd add(CommandScriptAdd command) {
        if (StringUtils.isAnyBlank(command.getUser_id(), command.getName())) {
            return returnException(ExceptionConstant.missing_param, ResponseScriptAdd.class);
        }

        if (CollectionUtils.isEmpty(command.getNodes())) {
            return returnException("List_nodes_empty", ResponseScriptAdd.class);
        }

        ScriptEntity script = ScriptEntity.builder()
                .name(command.getName())
                .userId((command.getUser_id()))
                .uiRendering(command.getUiRendering())
                .wrongMessage(command.getWrongMessage())
                .endMessage(command.getEndMessage())
                .description(command.getDescription())
                .createdDate(System.currentTimeMillis())
                .lastUpdatedDate(System.currentTimeMillis())
                .build();
        ScriptEntity addedScript = scriptRepository.insert(script);

        List<String> intentIds = new ArrayList<>();
        for (NodeEntity node : command.getNodes()) {
            node.setScriptId(script.getId());
            if (CollectionUtils.isNotEmpty(node.getConditionMappings())) {
                intentIds.addAll(node.getConditionMappings().stream().map(ConditionMappingEntity::getIntentId).filter(StringUtils::isNotBlank).toList());
            }
        }
        List<NodeEntity> addedNodes = nodeService.addMany(command.getNodes());
        if (CollectionUtils.isEmpty(addedNodes)) {
            scriptRepository.deleteById(script.getId());
            return returnException("Add_nodes_fail", ResponseScriptAdd.class);
        }
        addedScript.setNodes(addedNodes);

        // Thêm vào bảng script_intent_mapping
        if (CollectionUtils.isNotEmpty(intentIds)) {
            boolean addSuccess = scriptIntentMappingService.addForScriptIdByIntentIds(command.getUser_id(), script.getId(), intentIds);
            if (BooleanUtils.isFalse(addSuccess)) {
                return returnException("add_script_intent_mapping_fail", ResponseScriptAdd.class);
            }
        }

        return ResponseScriptAdd.builder()
                .script(addedScript)
                .build();
    }

    @Override
    public ResponseScriptGetByUserId getScriptByUserId(String userId) {
        if (StringUtils.isAnyBlank(userId)) {
            return returnException(ExceptionConstant.missing_param, ResponseScriptGetByUserId.class);
        }
        return ResponseScriptGetByUserId.builder()
                .scripts(scriptRepository.findByUserId(userId))
                .build();
    }

    @Override
    public ScriptEntity getScriptById(String id) {
        ScriptEntity script = scriptRepository.findById(id).orElse(null);
        if (script == null) {
            return null;
        }

        // Lay cac node cua script nay
        List<NodeEntity> nodes = nodeService.getAllByScriptId(script.getId());
        if (CollectionUtils.isNotEmpty(nodes)) {
            script.setNodes(nodes);
        }

        return script;
    }

    @Override
    public ResponseScript updateName(CommandScriptUpdate command) {
        ScriptEntity script = scriptRepository.findById(command.getId()).orElse(null);
        if (script == null) {
            return returnException(ExceptionConstant.item_not_found, ResponseScript.class);
        }

        if (StringUtils.isBlank(command.getName())) {
            return returnException(ExceptionConstant.missing_param, ResponseScript.class);
        }

        script.setName(command.getName());
        return ResponseScript.builder().script(scriptRepository.save(script)).build();
    }

    @Override
    public ResponseScript deleteScript(String id) {
        scriptRepository.deleteById(id);

        List<NodeEntity> nodes = nodeService.getAllByScriptId(id);
        if (!CollectionUtils.isEmpty(nodes)) {
            nodeService.deleteMany(nodes.stream().map(NodeEntity::getId).collect(Collectors.toList()));
        }

        return ResponseScript.builder()
                .script(ScriptEntity.builder()
                        .id(id)
                        .build())
                .build();
    }

    @Override
    public ResponseScript update(CommandScriptUpdate command) {
        if (StringUtils.isAnyEmpty(command.getId(), command.getUserId())) {
            return returnException(ExceptionConstant.missing_param, ResponseScript.class);
        }

        ScriptEntity existScript = scriptRepository.findById(command.getId()).orElse(null);
        if (existScript == null) {
            return returnException("script_null", ResponseScript.class);
        }

        List<NodeEntity> oldNodes = nodeService.getAllByScriptId(existScript.getId());

        List<String> intentIds = new ArrayList<>();
        List<NodeEntity> addedNodes = new ArrayList<>();
        if (!CollectionUtils.isEmpty(command.getNodes())) {
            for (NodeEntity node : command.getNodes()) {
                node.setScriptId(existScript.getId());
                if (CollectionUtils.isNotEmpty(node.getConditionMappings())) {
                    intentIds.addAll(node.getConditionMappings().stream().map(ConditionMappingEntity::getIntentId).filter(StringUtils::isNotBlank).toList());
                }
            }
            addedNodes = nodeService.addMany(command.getNodes());
        }

        if (!CollectionUtils.isEmpty(oldNodes)) {
            nodeService.deleteMany(oldNodes.stream().map(NodeEntity::getId).collect(Collectors.toList()));
        }

        // Update bảng script_intent_mapping
        if (CollectionUtils.isNotEmpty(intentIds)) {
            scriptIntentMappingService.deleteByScriptId(command.getUserId(), command.getId());
            boolean addSuccess = scriptIntentMappingService.addForScriptIdByIntentIds(command.getUserId(), command.getId(), intentIds);
            if (BooleanUtils.isFalse(addSuccess)) {
                return returnException("update_script_intent_mapping_fail", ResponseScript.class);
            }
        }

        existScript.setName(command.getName());
        existScript.setUserId(command.getUserId());
        existScript.setUiRendering(command.getUiRendering());
        existScript.setWrongMessage(command.getWrongMessage());
        existScript.setDescription(command.getDescription());
        existScript.setEndMessage(command.getEndMessage());
        existScript.setLastUpdatedDate(System.currentTimeMillis());
        ScriptEntity updatedScript = scriptRepository.save(existScript);
        updatedScript.setNodes(addedNodes);

        return ResponseScript.builder()
                .script(updatedScript)
                .build();
    }

    @Override
    @Deprecated
    public Paginated<ScriptEntity> getPaginationByUserId(String userId, int page, int size) {
        if (StringUtils.isBlank(userId)) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        long total = scriptRepository.countByUserId(userId);
        if (total == 0L) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        List<ScriptEntity> scripts = scriptRepository.findByUserId(userId, PageRequest.of(page, size));
        return new Paginated<>(scripts, page, size, total);
    }

    @Override
    public Paginated<ScriptEntity> getPagination(CommandGetListScript command) {
        if (StringUtils.isBlank(command.getUserId())) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        if (command.getPage() <= 0 || command.getSize() <= 0) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        long total = mongoTemplate.count(query, ScriptEntity.class);
        if (total == 0L) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        PageRequest pageRequest = PageRequest.of(command.getPage() - 1, command.getSize());
        query.with(pageRequest);
        List<ScriptEntity> scriptEntities = mongoTemplate.find(query, ScriptEntity.class);
        this.setViewForListScripts(scriptEntities, command);
        return new Paginated<>(scriptEntities, command.getPage(), command.getSize(), total);
    }

    @Override
    public List<ScriptEntity> getList(CommandGetListScript command) {
        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return null;
        }

        List<ScriptEntity> scripts = mongoTemplate.find(query, ScriptEntity.class);
        if (CollectionUtils.isEmpty(scripts)) {
            return null;
        }

        this.setViewForListScripts(scripts, command);
        return scripts;
    }

    private void setViewForListScripts(List<ScriptEntity> scriptEntities, CommandGetListScript command) {
        if (CollectionUtils.isEmpty(scriptEntities)) {
            return;
        }

        if (BooleanUtils.isFalse(command.isHasNodes()) || (CollectionUtils.isNotEmpty(command.getReturnFields()) && !command.getReturnFields().contains("nodes"))) {
            return;
        }

        for (ScriptEntity script : scriptEntities) {
            if (BooleanUtils.isTrue(command.isHasNodes()) && (CollectionUtils.isEmpty(command.getReturnFields()) || command.getReturnFields().contains("nodes"))) {
                script.setNodes(nodeService.getAllByScriptId(script.getId()));
            }
        }
    }

    private Query buildQueryGetList(CommandGetListScript command) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> orCriteriaList = new ArrayList<>();
        List<Criteria> andCriteriaList = new ArrayList<>();

        andCriteriaList.add(Criteria.where("user_id").is(command.getUserId()));

        if (StringUtils.isNotBlank(command.getKeyword())) {
            orCriteriaList.add(Criteria.where("name").regex(command.getKeyword().trim()));
        }

        if (CollectionUtils.isNotEmpty(command.getIds())) {
            andCriteriaList.add(Criteria.where("id").in(command.getIds()));
        }

        if (CollectionUtils.isNotEmpty(command.getDateFilters())) {
            for (DateFilter dateFilter : command.getDateFilters()) {
                if (dateFilter.getFromDate() != null &&
                        dateFilter.getToDate() != null &&
                        StringUtils.isNotBlank(dateFilter.getFieldName())) {
                    andCriteriaList.add(Criteria.where(dateFilter.getFieldName()).gte(dateFilter.getFromDate()).lte(dateFilter.getToDate()));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
            criteria.orOperator(orCriteriaList);
        }
        if (CollectionUtils.isNotEmpty(andCriteriaList)) {
            criteria.andOperator(andCriteriaList);
        }

        query.addCriteria(criteria);
        if (CollectionUtils.isNotEmpty(command.getReturnFields())) {
            List<String> returnFields = new ArrayList<>(command.getReturnFields());
            returnFields.remove("nodes");
            query.fields().include(Arrays.copyOf(returnFields.toArray(), returnFields.size(), String[].class));
        }
        return query;
    }

    @Override
    protected <T extends CommandGetListBase> Query buildQueryGetList(@NonNull T commandGetListBase) {
        return null;
    }

    @Override
    protected <Entity extends BaseEntity, Command extends CommandGetListBase> void setViews(List<Entity> entitiesBase, Command commandGetListBase) {

    }
}

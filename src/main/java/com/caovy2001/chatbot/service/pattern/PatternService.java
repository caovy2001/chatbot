package com.caovy2001.chatbot.service.pattern;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.*;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.repository.PatternRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.entity.IEntityService;
import com.caovy2001.chatbot.service.entity.command.CommandAddEntity;
import com.caovy2001.chatbot.service.entity.command.CommandEntityAddMany;
import com.caovy2001.chatbot.service.entity_type.IEntityTypeService;
import com.caovy2001.chatbot.service.entity_type.command.CommandEntityTypeAddMany;
import com.caovy2001.chatbot.service.intent.IIntentService;
import com.caovy2001.chatbot.service.intent.command.CommandGetListIntent;
import com.caovy2001.chatbot.service.intent.command.CommandIntentAddMany;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;
import com.caovy2001.chatbot.service.jedis.IJedisService;
import com.caovy2001.chatbot.service.pattern.command.*;
import com.caovy2001.chatbot.service.pattern.response.ResponseImportExcelStatus;
import com.caovy2001.chatbot.service.pattern.response.ResponsePattern;
import com.caovy2001.chatbot.service.pattern.response.ResponsePatternAdd;
import com.caovy2001.chatbot.service.script_intent_mapping.command.CommandGetListScriptIntentMapping;
import com.caovy2001.chatbot.utils.ChatbotStringUtils;
import com.caovy2001.chatbot.utils.ExcelUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class PatternService extends BaseService implements IPatternService {
    @Autowired
    private PatternRepository patternRepository;

    @Autowired
    private IIntentService intentService;

    @Autowired
    private IEntityService entityService;

    @Autowired
    private IEntityTypeService entityTypeService;

    @Autowired
    private IJedisService jedisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public ResponsePatternAdd add(CommandPatternAdd command) {
        if (StringUtils.isAnyBlank(command.getUserId(), command.getContent(), command.getIntentId())) {
            return returnException(ExceptionConstant.missing_param, ResponsePatternAdd.class);
        }

        PatternEntity pattern = PatternEntity.builder()
                .content(command.getContent())
                .intentId(command.getIntentId())
                .userId(command.getUserId())
                .build();
        PatternEntity addedPattern = patternRepository.insert(pattern);

        // Add entity of this pattern
        if (CollectionUtils.isNotEmpty(command.getEntities())) {
            CompletableFuture.runAsync(() -> {
                try {
                    this.addEntityForPattern(command.getEntities(), command.getUserId(), addedPattern.getId());
                } catch (Exception e) {
                    log.info(e.getMessage());
                }
            });
        }

        return ResponsePatternAdd.builder()
                .id(addedPattern.getId())
                .build();
    }

    private void addEntityForPattern(@NonNull List<CommandAddEntity> commandAddEntities, @NonNull String userId, @NonNull String patternId) {
        if (CollectionUtils.isEmpty(commandAddEntities)) {
            return;
        }
        List<CommandAddEntity> commandAddEntitiesToAdd = new ArrayList<>();
        for (CommandAddEntity commandAddEntity : commandAddEntities) {
            commandAddEntitiesToAdd.add(CommandAddEntity.builder()
                    .userId(userId)
                    .patternId(patternId)
                    .entityTypeId(commandAddEntity.getEntityTypeId())
                    .value(commandAddEntity.getValue())
                    .startPosition(commandAddEntity.getStartPosition())
                    .endPosition(commandAddEntity.getEndPosition())
                    .build());
        }

        entityService.add(commandAddEntitiesToAdd);
    }

    @Override
    public ResponsePattern delete(CommandPatternDelete command) {
        if (StringUtils.isAnyBlank(command.getId(), command.getUserId())) {
            return returnException(ExceptionConstant.missing_param, ResponsePattern.class);
        }

        PatternEntity pattern = patternRepository.findByIdAndUserId(command.getId(), command.getUserId());
        if (pattern == null) {
            return returnException("pattern_not_exist", ResponsePattern.class);
        }

        patternRepository.deleteById(command.getId());
        return ResponsePattern.builder()
                .pattern(PatternEntity.builder()
                        .id(command.getId())
                        .userId(command.getUserId())
                        .build())
                .build();
    }

    @Override
    public ResponsePattern getByIntentId(String intentId, String userId) {
        if (intentId == null) {
            return returnException(ExceptionConstant.missing_param, ResponsePattern.class);
        }
        return ResponsePattern.builder().patterns(patternRepository.findByIntentIdInAndUserId(intentId, userId)).build();
    }

    @Override
    @Deprecated
    public List<PatternEntity> addMany(List<PatternEntity> patternsToAdd) {
        return patternRepository.insert(patternsToAdd);
    }

    @Override
    public List<PatternEntity> addMany(@NonNull CommandPatternAddMany command) {
        if (StringUtils.isBlank(command.getUserId()) || CollectionUtils.isEmpty(command.getPatterns())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        command.getPatterns().forEach(patternEntity -> {
            patternEntity.setUserId(command.getUserId());
        });
        return patternRepository.saveAll(command.getPatterns());
    }

    @Override
    public ResponsePattern getById(String id, String userId) {
        if (StringUtils.isAnyBlank(id, userId)) {
            return returnException(ExceptionConstant.missing_param, ResponsePattern.class);
        }

        PatternEntity pattern = patternRepository.findByIdAndUserId(id, userId);
        if (pattern == null) {
            return returnException("pattern_not_exist", ResponsePattern.class);
        }

        return ResponsePattern.builder()
                .pattern(pattern)
                .entities(entityService.findByUserIdAndPatternId(userId, pattern.getId()))
                .build();
    }

    @Override
    public ResponsePattern getByUserId(String userId) {
        if (StringUtils.isBlank(userId)) {
            return returnException(ExceptionConstant.missing_param, ResponsePattern.class);
        }

        List<PatternEntity> patterns = patternRepository.findAllByUserId(userId);
        if (CollectionUtils.isEmpty(patterns)) {
            patterns = new ArrayList<>();
        }

        return ResponsePattern.builder()
                .patterns(patterns)
                .build();
    }

    @Override
    public ResponsePattern update(CommandPatternUpdate command) {
        if (StringUtils.isAnyBlank(command.getId(), command.getUserId())) {
            return returnException(ExceptionConstant.missing_param, ResponsePattern.class);
        }

        PatternEntity pattern = patternRepository.findByIdAndUserId(command.getId(), command.getUserId());
        if (pattern == null) {
            return returnException("pattern_not_exist", ResponsePattern.class);
        }

        pattern.setContent(command.getContent());
        PatternEntity updatedPattern = patternRepository.save(pattern);

        return ResponsePattern.builder()
                .pattern(updatedPattern)
                .build();
    }

    @Override
    public Paginated<PatternEntity> getPaginationByUserId(String userId, int page, int size) {
        if (StringUtils.isBlank(userId)) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        long total = patternRepository.countByUserId(userId);
        if (total == 0L) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        List<PatternEntity> patterns = patternRepository.findByUserId(userId, PageRequest.of(page, size));
        if (!CollectionUtils.isEmpty(patterns)) {
            for (PatternEntity pattern : patterns) {
                if (StringUtils.isNotBlank(pattern.getIntentId())) {
                    ResponseIntents responseIntents = intentService.getById(pattern.getIntentId(), pattern.getUserId());
                    if (responseIntents != null && responseIntents.getIntent() != null) {
                        pattern.setIntentName(responseIntents.getIntent().getName());
                    }
                }
            }
        }
        return new Paginated<>(patterns, page, size, total);
    }

    @Override
    public Paginated<PatternEntity> getPaginationByUserId(CommandGetListPattern command) {
        if (StringUtils.isBlank(command.getUserId())) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        if (command.getPage() <= 0) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        long total = mongoTemplate.count(query, PatternEntity.class);
        if (total == 0L) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        PageRequest pageRequest = PageRequest.of(command.getPage() - 1, command.getSize());
        query.with(pageRequest);
        List<PatternEntity> patternEntities = mongoTemplate.find(query, PatternEntity.class);
        return new Paginated<>(patternEntities, command.getPage(), command.getSize(), total);
    }

    @Override
    public Paginated<PatternEntity> getPaginationByIntentId(String intentId, int page, int size) {
        if (StringUtils.isBlank(intentId)) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        long total = patternRepository.countByIntentId(intentId);
        if (total == 0L) {
            return new Paginated<>(new ArrayList<>(), 0, 0, 0);
        }

        List<PatternEntity> patterns = patternRepository.findByIntentId(intentId, PageRequest.of(page, size));
        return new Paginated<>(patterns, page, size, total);
    }

    @Override
    public void importFromExcel(CommandImportPatternsFromExcel command) throws Exception {
//        long startTime = System.currentTimeMillis();
        if (StringUtils.isAnyBlank(command.getSessionId(), command.getUserId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        File importFile = null;
        Workbook workbook = null;

        ResponseImportExcelStatus response = ResponseImportExcelStatus.builder()
                .userId(command.getUserId())
                .sessionId(command.getSessionId())
                .numOfSuccess(0)
                .numOfFailed(0)
                .build();
        String importExcelJedisKey = Constant.JedisPrefix.userIdPrefix_ + command.getUserId() +
                Constant.JedisPrefix.COLON +
                Constant.JedisPrefix.Pattern.importExcelSessionIdPrefix_ + command.getSessionId();
        jedisService.setWithExpired(importExcelJedisKey, objectMapper.writeValueAsString(response), 60*24);

        try {
            String path_file = command.getSessionId() + ".xlsx";

            importFile = new File(path_file);
            workbook = new XSSFWorkbook(importFile);
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            Sheet mySheet = workbook.getSheetAt(0);

            List<IntentEntity> intentEntities = new ArrayList<>();
            List<PatternEntity> patternEntities = new ArrayList<>();
            List<EntityEntity> entityEntities = new ArrayList<>();
            List<EntityTypeEntity> entityTypeEntities = new ArrayList<>();

            int rowIndex = 0;
            for (Row currentRow : mySheet) {
                rowIndex = currentRow.getRowNum();
                if (rowIndex < 2) { // bỏ qua 2 row chứa title
                    continue;
                }

                // Check xem có phải là dòng trống không
                if (ExcelUtil.getInstance().isBlankLine(currentRow, 60)) {
                    break;
                }

                // Nếu 2 cột đầu tiên rỗng thì bỏ qua (do không có giá trị của pattern và intent)
                if (currentRow.getCell(0) == null || StringUtils.isBlank(currentRow.getCell(0).getStringCellValue()) ||
                        currentRow.getCell(1) == null || StringUtils.isBlank(currentRow.getCell(1).getStringCellValue())) {
                    response.setNumOfFailed(response.getNumOfFailed() + 1);
                    continue;
                }

                IntentEntity intentEntity = IntentEntity.builder().build();
                PatternEntity patternEntity = PatternEntity.builder().build();

                // Lấy intent từ excel
                intentEntity.setName(currentRow.getCell(1).getStringCellValue().trim());
                intentEntity.setCode(ChatbotStringUtils.stripAccents(currentRow.getCell(1).getStringCellValue().trim()).replace(" ", "_").toLowerCase());
                intentEntity.setUserId(command.getUserId());
                IntentEntity existIntent = intentEntities.stream().filter(intent -> intent.getCode().equals(intentEntity.getCode())).findFirst().orElse(null);
                if (existIntent == null) {
                    intentEntities.add(intentEntity);
                }

                // Lấy pattern từ excel
                patternEntity.setContent(currentRow.getCell(0).getStringCellValue().trim());
                patternEntity.setIntentCode(intentEntity.getCode());
                patternEntity.setUserId(command.getUserId());
                patternEntity.setUuid(UUID.randomUUID().toString());
                patternEntities.add(patternEntity);

                // Lấy entity type và entity từ excel
                for (int i = 1; i <= 10; i++) {
                    int entityCellIdx = 2 * i;
                    int entityTypeCellIdx = 2 * i + 1;
                    if (currentRow.getCell(entityCellIdx) == null ||
                            currentRow.getCell(entityTypeCellIdx) == null ||
                            StringUtils.isBlank(currentRow.getCell(entityCellIdx).getStringCellValue()) ||
                            StringUtils.isBlank(currentRow.getCell(entityTypeCellIdx).getStringCellValue())) {
                        continue;
                    }

                    EntityEntity entityEntity = EntityEntity.builder().build();
                    EntityTypeEntity entityTypeEntity = EntityTypeEntity.builder().build();

                    // Lấy entity type
                    String entityTypeUuid = UUID.randomUUID().toString();
                    entityTypeEntity.setName(currentRow.getCell(entityTypeCellIdx).getStringCellValue().trim());
                    entityTypeEntity.setUserId(command.getUserId());
                    entityTypeEntity.setUuid(entityTypeUuid);
                    EntityTypeEntity existEntityType = entityTypeEntities.stream()
                            .filter(e -> e.getName().equals(entityTypeEntity.getName())).findFirst().orElse(null);
                    if (existEntityType == null) {
                        entityTypeEntities.add(entityTypeEntity);
                    } else {
                        entityTypeUuid = existEntityType.getUuid();
                    }

                    // Lấy entity
                    entityEntity.setUserId(command.getUserId());
                    entityEntity.setEntityTypeUuid(entityTypeUuid);
                    entityEntity.setPatternUuid(patternEntity.getUuid());
                    entityEntity.setValue(currentRow.getCell(entityCellIdx).getStringCellValue().trim());
                    entityEntity.setStartPosition(patternEntity.getContent().indexOf(entityEntity.getValue()));
                    entityEntity.setEndPosition(entityEntity.getStartPosition() + entityEntity.getValue().length() - 1);
                    EntityEntity existEntity = entityEntities.stream()
                            .filter(e ->
                                    e.getPatternUuid().equals(entityEntity.getPatternUuid()) &&
                                            e.getStartPosition() == entityEntity.getStartPosition() &&
                                            e.getEndPosition() == entityEntity.getEndPosition()).findFirst().orElse(null);
                    if (existEntity == null) {
                        entityEntities.add(entityEntity);
                    }
                }

                // Cứ 10 patterns thì submit xuống db một lần
                if (patternEntities.size() == 10) {
                    this.saveObjectWhenImportExcel(command.getUserId(),
                            response,
                            intentEntities,
                            patternEntities,
                            entityTypeEntities,
                            entityEntities);

                    // Giải phóng bộ nhớ
                    intentEntities = new ArrayList<>();
                    patternEntities = new ArrayList<>();
                    entityTypeEntities = new ArrayList<>();
                    entityEntities = new ArrayList<>();

                    // Cập nhật trạng thái trên redis
                    jedisService.setWithExpired(importExcelJedisKey, objectMapper.writeValueAsString(response), 60*24);
                }
            }

            if (CollectionUtils.isNotEmpty(patternEntities)) {
                this.saveObjectWhenImportExcel(command.getUserId(),
                        response,
                        intentEntities,
                        patternEntities,
                        entityTypeEntities,
                        entityEntities);

                // Cập nhật trạng thái trên redis
                jedisService.setWithExpired(importExcelJedisKey, objectMapper.writeValueAsString(response), 60*24);
            }

//            System.out.println("Slow: " + (System.currentTimeMillis() - startTime));
        } catch (Throwable e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
        } finally {
            try {
                if (workbook != null) workbook.close();
                if (importFile != null) importFile.delete();
            } catch (IOException e) {
                log.error(e.getLocalizedMessage());
            }
        }
    }

    @Override
    public List<PatternEntity> getList(CommandGetListPattern command) {
        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        Query query = this.buildQueryGetList(command);
        if (query == null) {
            return null;
        }

        return mongoTemplate.find(query, PatternEntity.class);
    }

    private Query buildQueryGetList(CommandGetListPattern command) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> orCriteriaList = new ArrayList<>();
        List<Criteria> andCriteriaList = new ArrayList<>();

        andCriteriaList.add(Criteria.where("user_id").is(command.getUserId()));

        if (StringUtils.isNotBlank(command.getKeyword())) {
            orCriteriaList.add(Criteria.where("content").regex(command.getKeyword().trim(), "i"));
        }

        if (StringUtils.isNotBlank(command.getIntentId())) {
            andCriteriaList.add(Criteria.where("intent_id").is(command.getIntentId()));
        }

        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
            criteria.orOperator(orCriteriaList);
        }
        if (CollectionUtils.isNotEmpty(andCriteriaList)) {
            criteria.andOperator(andCriteriaList);
        }

        query.addCriteria(criteria);
        return query;
    }

    /** Hàm này chỉ được dùng cho hàm importFromExcel, không được dùng hàm này ở những hàm khác */
    @Deprecated
    private void saveObjectWhenImportExcel(@NonNull String userId,
                                           @NonNull ResponseImportExcelStatus response,
                                           @NonNull List<IntentEntity> intentEntities,
                                           @NonNull List<PatternEntity> patternEntities,
                                           @NonNull List<EntityTypeEntity> entityTypeEntities,
                                           @NonNull List<EntityEntity> entityEntities) {
        // Lưu intent
        CommandIntentAddMany commandIntentAddMany = CommandIntentAddMany.builder()
                .userId(userId)
                .intents(intentEntities)
                .build();
        List<IntentEntity> savedIntents = intentService.addManyReturnList(commandIntentAddMany);
        if (CollectionUtils.isEmpty(savedIntents)) {
            response.setNumOfFailed(response.getNumOfFailed() + patternEntities.size());
            return;
        }
        Map<String, IntentEntity> savedIntentsByCode = new HashMap<>();
        savedIntents.forEach(si -> {
            if (StringUtils.isNotBlank(si.getCode()) && StringUtils.isNotBlank(si.getId())) {
                savedIntentsByCode.put(si.getCode(), si);
            }
        });

        // Lưu pattern
        for (int i = 0; i < patternEntities.size(); i++) {
            int finalI = i;
            // Tìm kiếm intent đã lưu theo code -> lấy được intent_id để gán vào pattern
            IntentEntity savedExistIntent = savedIntentsByCode.get(patternEntities.get(finalI).getIntentCode());
            if (savedExistIntent != null) {
                patternEntities.get(i).setIntentId(savedExistIntent.getId());
            } else {
                patternEntities.remove(i);
                i--;
            }
        }
        CommandPatternAddMany commandPatternAddMany = CommandPatternAddMany.builder()
                .userId(userId)
                .patterns(patternEntities)
                .build();
        List<PatternEntity> savedPatterns = this.addMany(commandPatternAddMany);
        if (CollectionUtils.isEmpty(savedPatterns)) {
            response.setNumOfFailed(response.getNumOfFailed() + patternEntities.size());
            return;
        }

        if (CollectionUtils.isNotEmpty(entityTypeEntities) &&
                CollectionUtils.isNotEmpty(entityEntities)) {
            // Lưu entity type
            CommandEntityTypeAddMany commandEntityTypeAddMany = CommandEntityTypeAddMany.builder()
                    .userId(userId)
                    .entityTypes(entityTypeEntities)
                    .build();
            List<EntityTypeEntity> savedEntityTypes = entityTypeService.addMany(commandEntityTypeAddMany);

            if (CollectionUtils.isNotEmpty(savedEntityTypes)) {
                // Lưu entity
                for (int i = 0; i < entityEntities.size(); i++) {
                    int finalI = i;
                    // Tìm kiếm pattern đã lưu theo uuid -> lấy được pattern_id để gán vào entity
                    String patternUuid = entityEntities.get(finalI).getPatternUuid();
                    PatternEntity savedExistPattern = savedPatterns.stream()
                            .filter(p -> patternUuid.equals(p.getUuid())).findFirst().orElse(null);
                    if (savedExistPattern != null) {
                        entityEntities.get(i).setPatternId(savedExistPattern.getId());
                    } else {
                        entityEntities.remove(i);
                        i--;
                        continue;
                    }

                    // Tìm kiếm entity_type đã lưu theo uuid -> lấy được entity_type_id để gán vào entity
                    String entityTypeUuid = entityEntities.get(finalI).getEntityTypeUuid();
                    EntityTypeEntity savedExistEntityType = savedEntityTypes.stream()
                            .filter(et -> entityTypeUuid.equals(et.getUuid())).findFirst().orElse(null);
                    if (savedExistEntityType != null) {
                        entityEntities.get(i).setEntityTypeId(savedExistEntityType.getId());
                    } else {
                        entityEntities.remove(i);
                        i--;
                        continue;
                    }
                }
            }
        }

        CommandEntityAddMany commandEntityAddMany = CommandEntityAddMany.builder()
                .userId(userId)
                .entities(entityEntities)
                .build();
        entityService.addMany(commandEntityAddMany);
        response.setNumOfSuccess(response.getNumOfSuccess() + patternEntities.size());
    }
}

package com.caovy2001.chatbot.service.pattern;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.EntityEntity;
import com.caovy2001.chatbot.entity.EntityTypeEntity;
import com.caovy2001.chatbot.entity.IntentEntity;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.model.DateFilter;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.repository.PatternRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.entity.IEntityService;
import com.caovy2001.chatbot.service.entity.command.CommandAddEntity;
import com.caovy2001.chatbot.service.entity.command.CommandEntityAddMany;
import com.caovy2001.chatbot.service.entity.command.CommandGetListEntity;
import com.caovy2001.chatbot.service.entity_type.IEntityTypeService;
import com.caovy2001.chatbot.service.entity_type.command.CommandEntityTypeAddMany;
import com.caovy2001.chatbot.service.intent.IIntentService;
import com.caovy2001.chatbot.service.intent.command.CommandGetListIntent;
import com.caovy2001.chatbot.service.intent.command.CommandIntentAddMany;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;
import com.caovy2001.chatbot.service.jedis.IJedisService;
import com.caovy2001.chatbot.service.pattern.command.*;
import com.caovy2001.chatbot.service.pattern.response.ResponseExportExcelStatus;
import com.caovy2001.chatbot.service.pattern.response.ResponseImportExcelStatus;
import com.caovy2001.chatbot.service.pattern.response.ResponsePattern;
import com.caovy2001.chatbot.service.pattern.response.ResponsePatternAdd;
import com.caovy2001.chatbot.utils.ChatbotStringUtils;
import com.caovy2001.chatbot.utils.ExcelUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public ResponsePatternAdd add(CommandPatternAdd command) throws Exception {
        if (StringUtils.isAnyBlank(command.getUserId(), command.getContent(), command.getIntentId())) {
            return returnException(ExceptionConstant.missing_param, ResponsePatternAdd.class);
        }

        PatternEntity pattern = PatternEntity.builder()
                .content(command.getContent())
                .intentId(command.getIntentId())
                .userId(command.getUserId())
                .build();
        PatternEntity addedPattern = patternRepository.insert(pattern);

        // Index ES
        this.indexES(CommandIndexingPatternES.builder()
                .userId(command.getUserId())
                .patterns(List.of(addedPattern))
                .doSetUserId(false)
                .build());

        // Add entity of this pattern
        if (CollectionUtils.isNotEmpty(command.getEntities())) {
            CompletableFuture.runAsync(() -> {
                try {
                    this.addEntityForPattern(command.getEntities(), command.getUserId(), addedPattern.getId());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            });
        }

        return ResponsePatternAdd.builder()
                .id(addedPattern.getId())
                .build();
    }

    private List<EntityEntity> addEntityForPattern(@NonNull List<CommandAddEntity> commandAddEntities, @NonNull String userId, @NonNull String patternId) {
        if (CollectionUtils.isEmpty(commandAddEntities)) {
            return null;
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

        return entityService.add(commandAddEntitiesToAdd);
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

        if (BooleanUtils.isTrue(command.isDoDeleteEntities())) {
            entityService.delete(CommandGetListEntity.builder()
                    .userId(command.getUserId())
                    .patternId(command.getId())
                    .build());
        }
        return ResponsePattern.builder()
                .pattern(PatternEntity.builder()
                        .id(command.getId())
                        .userId(command.getUserId())
                        .build())
                .build();
    }

    @Override
    public boolean delete(CommandGetListPattern command) {
        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return false;
        }

        // Quyết định những trường trả về
        command.setReturnFields(List.of("id"));

        List<PatternEntity> patterns = this.getList(command);
        if (CollectionUtils.isEmpty(patterns)) {
            return false;
        }

        List<String> patternIds = patterns.stream().map(PatternEntity::getId).toList();
        if (CollectionUtils.isEmpty(patternIds)) {
            return false;
        }

        boolean result = patternRepository.deleteAllByIdIn(patternIds) > 0;
        if (BooleanUtils.isFalse(result)) {
            return false;
        }

        if (BooleanUtils.isTrue(command.isHasEntities())) {
            entityService.delete(CommandGetListEntity.builder()
                    .userId(command.getUserId())
                    .patternId(command.getId())
                    .build());
        }
        return result;
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
    public ResponsePattern update(CommandPatternUpdate command) throws Exception {
        if (StringUtils.isAnyBlank(command.getId(), command.getUserId())) {
            return returnException(ExceptionConstant.missing_param, ResponsePattern.class);
        }

        List<PatternEntity> patterns = this.getList(CommandGetListPattern.builder()
                .id(command.getId())
                .userId(command.getUserId())
                .hasEntities(true)
                .build());
        if (CollectionUtils.isEmpty(patterns)) {
            return returnException("pattern_not_exist", ResponsePattern.class);
        }

        PatternEntity pattern = patterns.get(0);
        pattern.setContent(command.getContent());
        PatternEntity updatedPattern = patternRepository.save(pattern);

        // Xóa entity cũ
        if (CollectionUtils.isNotEmpty(pattern.getEntities())) {
            boolean removeEntities = entityService.delete(CommandGetListEntity.builder()
                    .userId(command.getUserId())
                    .ids(pattern.getEntities().stream().map(EntityEntity::getId).toList())
                    .build());

            if (BooleanUtils.isFalse(removeEntities)) {
                throw new Exception("update_entities_fail");
            }
        }

        // Thêm entities mới
        if (CollectionUtils.isNotEmpty(command.getEntities())) {
            updatedPattern.setEntities(this.addEntityForPattern(command.getEntities(), command.getUserId(), pattern.getId()));
        }

        return ResponsePattern.builder()
                .pattern(updatedPattern)
                .build();
    }

    @Override
    public Paginated<PatternEntity> getPagination(String userId, int page, int size) {
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
    public Paginated<PatternEntity> getPagination(CommandGetListPattern command) {
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

        long total = mongoTemplate.count(query, PatternEntity.class);
        if (total == 0L) {
            return new Paginated<>(new ArrayList<>(), command.getPage(), command.getSize(), 0);
        }

        PageRequest pageRequest = PageRequest.of(command.getPage() - 1, command.getSize());
        query.with(pageRequest);
        List<PatternEntity> patternEntities = mongoTemplate.find(query, PatternEntity.class);
        this.setViewForListPatterns(patternEntities, command);
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
    public void importFromFile(CommandImportPatternsFromFile command) throws Exception {
//        long startTime = System.currentTimeMillis();
        if (StringUtils.isAnyBlank(command.getSessionId(), command.getUserId(), command.getSessionId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        // Check extension type
        if (!Arrays.asList("xls", "xlsx", "csv").contains(command.getExtensionType())) {
            throw new Exception("only_accept_xlsx_or_xls_or_csv");
        }

        ResponseImportExcelStatus response = ResponseImportExcelStatus.builder()
                .userId(command.getUserId())
                .sessionId(command.getSessionId())
                .numOfSuccess(0)
                .numOfFailed(0)
                .build();
        String importFileJedisKey = Constant.JedisPrefix.userIdPrefix_ + command.getUserId() +
                Constant.JedisPrefix.COLON +
                Constant.JedisPrefix.Pattern.importExcelSessionIdPrefix_ + command.getSessionId();
        jedisService.setWithExpired(importFileJedisKey, objectMapper.writeValueAsString(response), 60 * 24);

        if (Arrays.asList("xls", "xlsx").contains(command.getExtensionType())) { // File xls/xlsx
            this.importFromFile_Excel(command, importFileJedisKey, response);
        } else {
            this.importFromFile_CSV(command, importFileJedisKey, response);
        }

//            System.out.println("Slow: " + (System.currentTimeMillis() - startTime));
    }

    @Deprecated
    private void importFromFile_CSV(CommandImportPatternsFromFile command, String importFileJedisKey, ResponseImportExcelStatus response) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(command.getSessionId() + "." + command.getExtensionType()));
            List<IntentEntity> intentEntities = new ArrayList<>();
            List<PatternEntity> patternEntities = new ArrayList<>();
            List<EntityEntity> entityEntities = new ArrayList<>();
            List<EntityTypeEntity> entityTypeEntities = new ArrayList<>();

            String nextLine;
            int rowIndex = 0;
            while ((nextLine = br.readLine()) != null) {
                if (rowIndex < 2) { // bỏ qua 2 row chứa title
                    rowIndex++;
                    continue;
                }

                String[] rowData = nextLine.split(",");

                // Check xem có phải là dòng trống không
                if (rowData.length == 0) {
                    break;
                }

                // Nếu 2 cột đầu tiên rỗng thì bỏ qua (do không có giá trị của pattern và intent)
                if (rowData.length < 2 ||
                        StringUtils.isBlank(rowData[0]) ||
                        StringUtils.isBlank(rowData[1])) {
                    response.setNumOfFailed(response.getNumOfFailed() + 1);
                    rowIndex++;
                    continue;
                }

                IntentEntity intentEntity = IntentEntity.builder().build();
                PatternEntity patternEntity = PatternEntity.builder().build();

                // Lấy intent từ excel
                intentEntity.setName(rowData[1].trim());
                intentEntity.setCode(ChatbotStringUtils.stripAccents(rowData[1].trim()).replace(" ", "_").toLowerCase());
                intentEntity.setUserId(command.getUserId());
                IntentEntity existIntent = intentEntities.stream().filter(intent -> intent.getCode().equals(intentEntity.getCode())).findFirst().orElse(null);
                if (existIntent == null) {
                    intentEntities.add(intentEntity);
                }

                // Lấy pattern từ excel
                patternEntity.setContent(rowData[0].trim());
                patternEntity.setIntentCode(intentEntity.getCode());
                patternEntity.setUserId(command.getUserId());
                patternEntity.setUuid(UUID.randomUUID().toString());
                patternEntities.add(patternEntity);

                // Lấy entity type và entity từ excel
                for (int i = 1; i <= 10; i++) {
                    int entityCellIdx = 2 * i;
                    int entityTypeCellIdx = 2 * i + 1;
                    if (rowData.length < entityCellIdx + 1 ||
                            StringUtils.isBlank(rowData[entityCellIdx]) ||
                            StringUtils.isBlank(rowData[entityTypeCellIdx])) {
                        continue;
                    }

                    EntityEntity entityEntity = EntityEntity.builder().build();
                    EntityTypeEntity entityTypeEntity = EntityTypeEntity.builder().build();

                    // Lấy entity type
                    String entityTypeUuid = UUID.randomUUID().toString();
                    entityTypeEntity.setName(rowData[entityTypeCellIdx].trim());
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
                    entityEntity.setValue(rowData[entityCellIdx].trim());
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
                    this.saveObjectWhenImportFile(command.getUserId(),
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
                    jedisService.setWithExpired(importFileJedisKey, objectMapper.writeValueAsString(response), 60 * 24);
                }

                rowIndex++;
            }

            if (CollectionUtils.isNotEmpty(patternEntities)) {
                this.saveObjectWhenImportFile(command.getUserId(),
                        response,
                        intentEntities,
                        patternEntities,
                        entityTypeEntities,
                        entityEntities);
            }
            // Cập nhật trạng thái trên redis => DONE
            response.setStatus(ResponseImportExcelStatus.EImportExcelStatus.DONE);
            jedisService.setWithExpired(importFileJedisKey, objectMapper.writeValueAsString(response), 60 * 24);
        } catch (Throwable e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
        }
    }

    @Deprecated
    private void importFromFile_Excel(CommandImportPatternsFromFile command, String importFileJedisKey, ResponseImportExcelStatus response) {
        File importFile = null;
        Workbook workbook = null;

        try {
            List<IntentEntity> intentEntities = new ArrayList<>();
            List<PatternEntity> patternEntities = new ArrayList<>();
            List<EntityEntity> entityEntities = new ArrayList<>();
            List<EntityTypeEntity> entityTypeEntities = new ArrayList<>();

            importFile = new File(command.getSessionId() + "." + command.getExtensionType());
            workbook = new XSSFWorkbook(importFile);
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            Sheet mySheet = workbook.getSheetAt(0);

            for (Row currentRow : mySheet) {
                int rowIndex = currentRow.getRowNum();
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
                    this.saveObjectWhenImportFile(command.getUserId(),
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
                    jedisService.setWithExpired(importFileJedisKey, objectMapper.writeValueAsString(response), 60 * 24);
                }
            }

            if (CollectionUtils.isNotEmpty(patternEntities)) {
                this.saveObjectWhenImportFile(command.getUserId(),
                        response,
                        intentEntities,
                        patternEntities,
                        entityTypeEntities,
                        entityEntities);
            }

            // Cập nhật trạng thái trên redis => DONE
            response.setStatus(ResponseImportExcelStatus.EImportExcelStatus.DONE);
            jedisService.setWithExpired(importFileJedisKey, objectMapper.writeValueAsString(response), 60 * 24);
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

        if (BooleanUtils.isTrue(command.getCheckPageAndSize())) {
            query.with(PageRequest.of(command.getPage() - 1, command.getSize()));
        }
        List<PatternEntity> patterns = mongoTemplate.find(query, PatternEntity.class);
        this.setViewForListPatterns(patterns, command);
        return patterns;
    }

    @Override
    public void exportExcel(CommandGetListPattern command, String sessionId) throws Exception {
        //region Set init status
        ResponseExportExcelStatus response = ResponseExportExcelStatus.builder()
                .sessionId(sessionId)
                .userId(command.getUserId())
                .numOfSuccess(0)
                .numOfFailed(0)
                .status(ResponseExportExcelStatus.EExportExcelStatus.PROCESSING)
                .build();

        String exportExcelJedisKey = Constant.JedisPrefix.userIdPrefix_ + command.getUserId() +
                Constant.JedisPrefix.COLON +
                Constant.JedisPrefix.Pattern.exportExcelSessionIdPrefix_ + sessionId;
        jedisService.setWithExpired(exportExcelJedisKey, objectMapper.writeValueAsString(response), 60 * 24);
        //endregion

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Training_data");

        //region Setup header style
        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeight(11);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        //endregion

        //region Set Header
        Row row1 = sheet.createRow(0);
        Row row2 = sheet.createRow(1);

        // Pattern
        Cell cellPatternHeader = row1.createCell(0);
        cellPatternHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 0));
        sheet.setColumnWidth(0, 25 * 256);
        cellPatternHeader.setCellValue("Pattern");

        // Intent
        Cell cellIntentHeader = row1.createCell(1);
        cellIntentHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 1, 1));
        sheet.setColumnWidth(1, 20 * 256);
        cellIntentHeader.setCellValue("Intent");

        // Entity
        Cell cellEntityHeader = row1.createCell(2);
        cellEntityHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 2, 9));
        sheet.setColumnWidth(2, 20 * 256);
        cellEntityHeader.setCellValue("Entity");

        // Entity 1
        Cell cellEntity1Header = row2.createCell(2);
        cellEntity1Header.setCellStyle(headerStyle);
        sheet.setColumnWidth(2, 20 * 256);
        cellEntity1Header.setCellValue("Entity 1");

        // Entity type 1
        Cell cellEntityType1Header = row2.createCell(3);
        cellEntityType1Header.setCellStyle(headerStyle);
        sheet.setColumnWidth(3, 20 * 256);
        cellEntityType1Header.setCellValue("Entity type 1");

        // Entity 2
        Cell cellEntity2Header = row2.createCell(4);
        cellEntity2Header.setCellStyle(headerStyle);
        sheet.setColumnWidth(4, 20 * 256);
        cellEntity2Header.setCellValue("Entity 2");

        // Entity type 2
        Cell cellEntityType2Header = row2.createCell(5);
        cellEntityType2Header.setCellStyle(headerStyle);
        sheet.setColumnWidth(5, 20 * 256);
        cellEntityType2Header.setCellValue("Entity type 2");

        // Entity 3
        Cell cellEntity3Header = row2.createCell(6);
        cellEntity3Header.setCellStyle(headerStyle);
        sheet.setColumnWidth(6, 20 * 256);
        cellEntity3Header.setCellValue("Entity 3");

        // Entity type 3
        Cell cellEntityType3Header = row2.createCell(7);
        cellEntityType3Header.setCellStyle(headerStyle);
        sheet.setColumnWidth(7, 20 * 256);
        cellEntityType3Header.setCellValue("Entity type 3");

        // Entity 4
        Cell cellEntity4Header = row2.createCell(8);
        cellEntity4Header.setCellStyle(headerStyle);
        sheet.setColumnWidth(8, 20 * 256);
        cellEntity4Header.setCellValue("Entity 4");

        // Entity type 4
        Cell cellEntityType4Header = row2.createCell(9);
        cellEntityType4Header.setCellStyle(headerStyle);
        sheet.setColumnWidth(9, 20 * 256);
        cellEntityType4Header.setCellValue("Entity type 4");
        //endregion

        //region Set data

        //region Setup content style
        CellStyle contentStyleForPattern = workbook.createCellStyle();
        XSSFFont contentFont = workbook.createFont();
        contentFont.setFontHeight(11);
        contentStyleForPattern.setFont(contentFont);
        contentStyleForPattern.setAlignment(HorizontalAlignment.LEFT);

        CellStyle contentStyle = workbook.createCellStyle();
        contentStyle.setFont(contentFont);
        contentStyle.setAlignment(HorizontalAlignment.CENTER);
        //endregion

        int sizeOfIntentsPerTime = 10;
        int sizeOfPatternsPerTime = 10;
        for (int intentPage = 1; ; intentPage++) {
            CommandGetListIntent commandGetListIntent = CommandGetListIntent.builder()
                    .userId(command.getUserId())
                    .returnFields(List.of("id", "name"))
                    .page(intentPage)
                    .size(sizeOfIntentsPerTime)
                    .checkPageAndSize(true)
                    .build();
            List<IntentEntity> intents = intentService.getList(commandGetListIntent);
            if (CollectionUtils.isEmpty(intents)) {
                break;
            }

            int _rowCount = 2;
            for (IntentEntity intent : intents) {
                for (int patternPage = 1; ; patternPage++) {
                    CommandGetListPattern commandNew = null;
                    try {
                        commandNew = objectMapper.readValue(objectMapper.writeValueAsString(command), CommandGetListPattern.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    if (commandNew == null) {
                        break;
                    }

                    commandNew.setIntentId(intent.getId());
                    commandNew.setCheckPageAndSize(true);
                    commandNew.setPage(patternPage);
                    commandNew.setSize(10);
                    commandNew.setHasEntities(true);
                    commandNew.setHasEntityTypeOfEntities(true);
                    List<PatternEntity> patterns = this.getList(commandNew);
                    if (CollectionUtils.isEmpty(patterns)) {
                        break;
                    }

                    for (int z = 0; z < patterns.size(); z++, _rowCount++) {
                        PatternEntity pattern = patterns.get(z);
                        Row contentRow = sheet.createRow(_rowCount);

                        // Pattern
                        Cell cellPattern = contentRow.createCell(0);
                        cellPattern.setCellStyle(contentStyleForPattern);
                        cellPattern.setCellValue(pattern.getContent());

                        // Intent
                        Cell cellIntent = contentRow.createCell(1);
                        cellIntent.setCellStyle(contentStyle);
                        cellIntent.setCellValue(intent.getName());

                        // Entities
                        if (CollectionUtils.isNotEmpty(pattern.getEntities())) {
                            for (int k = 0; k < pattern.getEntities().size(); k++) {
                                EntityEntity entity = pattern.getEntities().get(k);

                                // Entity
                                if (StringUtils.isNotBlank(entity.getValue())) {
                                    Cell cellEntity = contentRow.createCell(k * 2 + 2);
                                    cellEntity.setCellStyle(contentStyle);
                                    cellEntity.setCellValue(entity.getValue());
                                }

                                // Entity type
                                if (entity.getEntityType() != null &&
                                        StringUtils.isNotBlank(entity.getEntityType().getName())) {
                                    Cell cellEntityType = contentRow.createCell(k * 2 + 3);
                                    cellEntityType.setCellStyle(contentStyle);
                                    cellEntityType.setCellValue(entity.getEntityType().getName());
                                }

                            }
                        }
                    }

                    response.setNumOfSuccess(response.getNumOfSuccess() + patterns.size());
                    jedisService.setWithExpired(exportExcelJedisKey, objectMapper.writeValueAsString(response), 60 * 24);
                    if (patterns.size() != sizeOfPatternsPerTime) {
                        break;
                    }
                }
            }

            if (intents.size() != sizeOfIntentsPerTime) {
                break;
            }
        }

        String fileName = "Training_data_" + sessionId + "_" + String.valueOf(System.currentTimeMillis()) + ".xlsx";
        String filePath = "src/main/resources/file_data/" + command.getUserId() + "/";
        response.setStatus(ResponseExportExcelStatus.EExportExcelStatus.DONE);
        response.setFileName(fileName);
        jedisService.setWithExpired(exportExcelJedisKey, objectMapper.writeValueAsString(response), 60 * 24);
        //endregion

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
            file = new File(filePath + fileName);
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file, false);
            workbook.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setViewForListPatterns(List<PatternEntity> patterns, CommandGetListPattern command) {
        if (BooleanUtils.isFalse(command.isHasEntities()) &&
                BooleanUtils.isFalse(command.isHasIntentName())) {
            return;
        }

        Map<String, IntentEntity> intentsById = new HashMap<>();
        if (BooleanUtils.isTrue(command.isHasIntentName())) {
            Set<String> intentIds = patterns.stream().map(PatternEntity::getIntentId).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
            List<IntentEntity> intents = intentService.getList(CommandGetListIntent.builder()
                    .userId(command.getUserId())
                    .ids(intentIds.stream().toList())
                    .build());
            if (CollectionUtils.isNotEmpty(intents)) {
                intents.forEach(i -> {
                    intentsById.put(i.getId(), i);
                });
            }
        }

        for (PatternEntity pattern : patterns) {
            if (BooleanUtils.isTrue(command.isHasEntities())) {
                List<EntityEntity> entities = entityService.getList(CommandGetListEntity.builder()
                        .userId(command.getUserId())
                        .patternId(pattern.getId())
                        .hasEntityType(command.isHasEntityTypeOfEntities())
                        .build());
                pattern.setEntities(entities);
            }

            if (BooleanUtils.isTrue(command.isHasIntentName())) {
                IntentEntity intent = intentsById.get(pattern.getIntentId());
                if (intent != null) {
                    pattern.setIntentName(intent.getName());
                }
            }
        }
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

        if (StringUtils.isNotBlank(command.getId())) {
            andCriteriaList.add(Criteria.where("id").is(command.getId()));
        }

        if (StringUtils.isNotBlank(command.getIntentId())) {
            andCriteriaList.add(Criteria.where("intent_id").is(command.getIntentId()));
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
            returnFields.removeAll(Collections.singletonList("entities"));
            returnFields.addAll(List.of("created_date", "last_updated_date"));
            query.fields().include(Arrays.copyOf(returnFields.toArray(), returnFields.size(), String[].class));
        }

        return query;
    }

    /**
     * Hàm này chỉ được dùng cho hàm importFromFile, không được dùng hàm này ở những hàm khác
     */
    @Deprecated
    private void saveObjectWhenImportFile(@NonNull String userId,
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

        if (CollectionUtils.isNotEmpty(entityEntities)) {
            CommandEntityAddMany commandEntityAddMany = CommandEntityAddMany.builder()
                    .userId(userId)
                    .entities(entityEntities)
                    .build();
            entityService.addMany(commandEntityAddMany);
        }
        response.setNumOfSuccess(response.getNumOfSuccess() + patternEntities.size());
    }

    private void indexES(CommandIndexingPatternES command) {
        try {
            // Đẩy vào kafka để index lên ES
            kafkaTemplate.send(Constant.KafkaTopic.process_indexing_pattern_es, objectMapper.writeValueAsString(command));
        } catch (JsonProcessingException e) {
            log.error("[{}]: {}", e.getStackTrace()[0], StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ExceptionConstant.error_occur);
        }
    }
}

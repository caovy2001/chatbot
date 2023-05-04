package com.caovy2001.chatbot.service.pattern;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.*;
import com.caovy2001.chatbot.model.DateFilter;
import com.caovy2001.chatbot.repository.PatternRepository;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.common.command.CommandAddBase;
import com.caovy2001.chatbot.service.common.command.CommandAddManyBase;
import com.caovy2001.chatbot.service.common.command.CommandGetListBase;
import com.caovy2001.chatbot.service.common.command.CommandUpdateBase;
import com.caovy2001.chatbot.service.entity.IEntityService;
import com.caovy2001.chatbot.service.entity.command.CommandAddEntity;
import com.caovy2001.chatbot.service.entity.command.CommandEntityAddMany;
import com.caovy2001.chatbot.service.entity.command.CommandGetListEntity;
import com.caovy2001.chatbot.service.entity_type.IEntityTypeService;
import com.caovy2001.chatbot.service.entity_type.command.CommandEntityTypeAddMany;
import com.caovy2001.chatbot.service.intent.IIntentService;
import com.caovy2001.chatbot.service.intent.command.CommandGetListIntent;
import com.caovy2001.chatbot.service.intent.command.CommandIntentAddMany;
import com.caovy2001.chatbot.service.jedis.IJedisService;
import com.caovy2001.chatbot.service.pattern.command.*;
import com.caovy2001.chatbot.service.pattern.response.ResponseExportExcelStatus;
import com.caovy2001.chatbot.service.pattern.response.ResponseImportExcelStatus;
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
    public <Entity extends BaseEntity, CommandAdd extends CommandAddBase> Entity add(CommandAdd commandAddBase) throws Exception {
        CommandPatternAdd command = (CommandPatternAdd) commandAddBase;
        if (StringUtils.isAnyBlank(command.getUserId(), command.getContent(), command.getIntentId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        String uuid = UUID.randomUUID().toString();

        // set UUID của pattern cho list entity
        if (command.getCommandEntityAddMany() != null &&
                CollectionUtils.isNotEmpty(command.getCommandEntityAddMany().getEntities())) {
            command.getCommandEntityAddMany().getEntities().forEach(entity -> entity.setPatternUuid(uuid));
        }

        CommandPatternAddMany commandPatternAddMany = CommandPatternAddMany.builder()
                .userId(command.getUserId())
                .patterns(List.of(PatternEntity.builder()
                        .content(command.getContent())
                        .intentId(command.getIntentId())
                        .userId(command.getUserId())
                        .uuid(uuid)
                        .build()))
                .commandEntityAddMany(command.getCommandEntityAddMany())
                .build();

        List<PatternEntity> addedPatterns = this.add(commandPatternAddMany);

        if (CollectionUtils.isEmpty(addedPatterns)) {
            throw new Exception("add_pattern_fail");
        }

        return (Entity) addedPatterns.get(0);
    }

    @Override
    public <Entity extends BaseEntity, CommandAddMany extends CommandAddManyBase> List<Entity> add(CommandAddMany commandAddManyBase) throws Exception {
        CommandPatternAddMany command = (CommandPatternAddMany) commandAddManyBase;

        if (StringUtils.isBlank(command.getUserId()) || CollectionUtils.isEmpty(command.getPatterns())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return null;
        }

        List<PatternEntity> patternsToAdd = new ArrayList<>();
        for (PatternEntity pattern : command.getPatterns()) {
            if (StringUtils.isBlank(pattern.getUuid())) {
                log.warn("[{}]: {}", new Exception().getStackTrace()[0], "pattern uuid null -> ignore to save");
                continue;
            }
            pattern.setUserId(command.getUserId());
            pattern.setCreatedDate(System.currentTimeMillis());
            pattern.setLastCreatedDate(System.currentTimeMillis());
            if (pattern.checkIsValid()) {
                patternsToAdd.add(pattern);
            }
        }
        List<PatternEntity> savedPatterns = patternRepository.saveAll(patternsToAdd);
        if (CollectionUtils.isEmpty(savedPatterns)) {
            throw new Exception("save_pattern_fail");
        }

        // Index ES
        CompletableFuture.runAsync(() -> {
            this.indexES(CommandIndexingPatternES.builder()
                    .userId(command.getUserId())
                    .patterns(savedPatterns)
                    .doSetUserId(false)
                    .build());
        });

        if (command.getCommandEntityAddMany() == null ||
                CollectionUtils.isEmpty(command.getCommandEntityAddMany().getEntities())) {
            return (List<Entity>) savedPatterns;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Save entities
                Map<String, PatternEntity> patternByUuid = new HashMap<>();
                savedPatterns.forEach(pattern -> patternByUuid.put(pattern.getUuid(), pattern));
                this.addEntityForPatterns(command.getCommandEntityAddMany(), patternByUuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return (List<Entity>) savedPatterns;
    }

    @Override
    public <Entity extends BaseEntity, CommandUpdate extends CommandUpdateBase> Entity update(CommandUpdate commandUpdateBase) throws Exception {
        CommandPatternUpdate command = (CommandPatternUpdate) commandUpdateBase;

        if (StringUtils.isAnyBlank(command.getId(), command.getUserId())) {
            throw new Exception(ExceptionConstant.missing_param);
        }

        List<PatternEntity> patterns = this.getList(CommandGetListPattern.builder()
                .id(command.getId())
                .userId(command.getUserId())
                .hasEntities(true)
                .build(), PatternEntity.class);
        if (CollectionUtils.isEmpty(patterns)) {
            throw new Exception("pattern_not_exist");
        }

        PatternEntity pattern = patterns.get(0);
        pattern.setContent(command.getContent());
        if (StringUtils.isBlank(pattern.getUuid())) {
            pattern.setUuid(UUID.randomUUID().toString());
        }
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
            command.getEntities().forEach(entity -> entity.setPatternUuid(updatedPattern.getUuid())); // set uuid của pattern cho entity để truyền vào hàm addEntityForPatterns
            Map<String, PatternEntity> patternByUuid = new HashMap<>();
            patternByUuid.put(updatedPattern.getUuid(), updatedPattern);

            updatedPattern.setEntities(this.addEntityForPatterns(CommandEntityAddMany.builder()
                    .userId(command.getUserId())
                    .entities(command.getEntities())
                    .build(), patternByUuid));
        }

        return (Entity) updatedPattern;
    }

    @Override
    public <CommandGetList extends CommandGetListBase> boolean delete(CommandGetList commandGetListBase) throws Exception {
        CommandGetListPattern command = (CommandGetListPattern) commandGetListBase;

        if (StringUtils.isBlank(command.getUserId())) {
            log.error("[{}]: {}", new Exception().getStackTrace()[0], ExceptionConstant.missing_param);
            return false;
        }

        // Quyết định những trường trả về
        command.setReturnFields(List.of("id"));

        List<PatternEntity> patterns = this.getList(command, PatternEntity.class);
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

    private List<EntityEntity> addEntityForPatterns(@NonNull CommandEntityAddMany command,
                                                    @NonNull Map<String, PatternEntity> patternByUuid) throws Exception{

        if (CollectionUtils.isEmpty(command.getEntities()) ||
                patternByUuid.isEmpty()) {
            return null;
        }

        // set pattern_id cho entity từ pattern_uuid
        for (EntityEntity entity : command.getEntities()) {
            if (StringUtils.isBlank(entity.getPatternUuid())) {
                log.warn("[{}]: {}", new Exception().getStackTrace()[0], "cannot find pattern_uuid -> ignore saving entity");
                continue;
            }
            if (BooleanUtils.isTrue(entity.checkIsValid())) {
                log.warn("[{}]: {}", new Exception().getStackTrace()[0], "entity invalid -> ignore saving entity");
                continue;
            }

            PatternEntity pattern = patternByUuid.get(entity.getPatternUuid());
            if (pattern == null) {
                log.warn("[{}]: {}", new Exception().getStackTrace()[0], "cannot find pattern having uuid -> ignore saving entity");
                continue;
            }
            entity.setPatternId(pattern.getId());
        }

        return entityService.add(command);
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
                intentEntity.setCode(ChatbotStringUtils.stripAccents(currentRow.getCell(1).getStringCellValue().trim()).replace(" ", "_"));
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
            List<IntentEntity> intents = intentService.getList(commandGetListIntent, IntentEntity.class);
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
                    List<PatternEntity> patterns = this.getList(commandNew, PatternEntity.class);
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

        String fileName = Constant.Pattern.exportExcelFileNamePrefix + sessionId + "_" + System.currentTimeMillis() + ".xlsx";
        String filePath = Constant.fileDataPath + command.getUserId() + "/";
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

    /**
     * Hàm này chỉ được dùng cho hàm importFromFile, không được dùng hàm này ở những hàm khác
     */
    @Deprecated
    private void saveObjectWhenImportFile(@NonNull String userId,
                                          @NonNull ResponseImportExcelStatus response,
                                          @NonNull List<IntentEntity> intentEntities,
                                          @NonNull List<PatternEntity> patternEntities,
                                          @NonNull List<EntityTypeEntity> entityTypeEntities,
                                          @NonNull List<EntityEntity> entityEntities) throws Exception{
        // Lưu intent
        CommandIntentAddMany commandIntentAddMany = CommandIntentAddMany.builder()
                .userId(userId)
                .intents(intentEntities)
                .returnSameCodeIntent(true)
                .build();
        List<IntentEntity> savedIntents = null;
        try {
            savedIntents = intentService.add(commandIntentAddMany);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        List<PatternEntity> savedPatterns = null;
        try {
            savedPatterns = this.add(commandPatternAddMany);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            entityService.add(commandEntityAddMany);
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

    @Override
    protected <T extends CommandGetListBase> Query buildQueryGetList(@NonNull T commandGetListBase) {
        CommandGetListPattern command = (CommandGetListPattern) commandGetListBase;

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

        if (CollectionUtils.isNotEmpty(command.getIds())) {
            andCriteriaList.add(Criteria.where("id").in(command.getIds()));
        }

        if (StringUtils.isNotBlank(command.getIntentId())) {
            andCriteriaList.add(Criteria.where("intent_id").is(command.getIntentId()));
        }

        if (CollectionUtils.isNotEmpty(command.getIntentIds())) {
            andCriteriaList.add(Criteria.where("intent_id").in(command.getIntentIds()));
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

    @Override
    protected <Entity extends BaseEntity, Command extends CommandGetListBase> void setViews(List<Entity> entitiesBase, Command commandGetListBase) {
        if (CollectionUtils.isEmpty(entitiesBase)) return;
        List<PatternEntity> patterns = (List<PatternEntity>) entitiesBase;
        CommandGetListPattern command = (CommandGetListPattern) commandGetListBase;

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
                    .build(), IntentEntity.class);
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
                        .build(), EntityEntity.class);
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
}

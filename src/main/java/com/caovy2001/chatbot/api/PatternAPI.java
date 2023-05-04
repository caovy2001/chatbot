package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.entity.command.CommandEntityAddMany;
import com.caovy2001.chatbot.service.jedis.IJedisService;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.*;
import com.caovy2001.chatbot.service.pattern.response.ResponsePattern;
import com.caovy2001.chatbot.service.pattern.response.ResponsePatternAdd;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
@RequestMapping("/pattern")
public class PatternAPI {
    @Autowired
    private IPatternService patternService;

    @Autowired
    private IJedisService jedisService;

    @Autowired
    private ObjectMapper objectMapper;

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/add")
    public ResponseEntity<ResponsePatternAdd> add(@RequestBody CommandPatternAdd command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            command.setUserId(userEntity.getId());
            if (CollectionUtils.isNotEmpty(command.getEntities())) {
                command.setCommandEntityAddMany(CommandEntityAddMany.builder()
                        .userId(command.getUserId())
                        .entities(command.getEntities())
                        .build());
            }
            PatternEntity pattern = patternService.add(command);
            if (pattern == null) {
                throw new Exception(ExceptionConstant.error_occur);
            }

            return ResponseEntity.ok(ResponsePatternAdd.builder()
                    .id(pattern.getId())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(patternService.returnException(e.getMessage(), ResponsePatternAdd.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/update")
    public ResponseEntity<ResponsePattern> update(@RequestBody CommandPatternUpdate command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            if (command == null || StringUtils.isBlank(command.getId())) {
                throw new Exception(ExceptionConstant.missing_param);
            }

            command.setUserId(userEntity.getId());
            return ResponseEntity.ok(ResponsePattern.builder()
                    .pattern(patternService.update(command))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(patternService.returnException(e.toString(), ResponsePattern.class));
        }
    }

//    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
//    @GetMapping("/get_all/by_intent_id/{intentId}")
//    public ResponseEntity<ResponsePattern> getByIntentId(@PathVariable String intentId) {
//        try {
//            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
//                throw new Exception("auth_invalid");
//
//            ResponsePattern patterns = patternService.getByIntentId(intentId, userEntity.getId());
//            return ResponseEntity.ok(patterns);
//        } catch (Exception e) {
//            return ResponseEntity.ok(patternService.returnException(e.toString(), ResponsePattern.class));
//        }
//    }

//    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
//    @GetMapping("/get_all/by_user_id")
//    public ResponseEntity<ResponsePattern> getByUserId() {
//        try {
//            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
//                throw new Exception("auth_invalid");
//
//            ResponsePattern patterns = patternService.getByUserId(userEntity.getId());
//            return ResponseEntity.ok(patterns);
//        } catch (Exception e) {
//            return ResponseEntity.ok(patternService.returnException(e.toString(), ResponsePattern.class));
//        }
//    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get/{id}")
    public ResponseEntity<Document> getById(@PathVariable String id) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            List<PatternEntity> patterns = patternService.getList(CommandGetListPattern.builder()
                    .hasEntities(true)
                    .userId(userEntity.getId())
                    .id(id)
                    .build(), PatternEntity.class);
            if (CollectionUtils.isEmpty(patterns)) {
                throw new Exception("pattern_not_exist");
            }

            Document resMap = objectMapper.readValue(objectMapper.writeValueAsString(patterns.get(0)), Document.class);
            resMap.put("http_status", "OK");
            return ResponseEntity.ok(resMap);
        } catch (Exception e) {
            Document resMap = new Document();
            resMap.put("http_status", "EXPECTATION_FAILED");
            resMap.put("exception_code", StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ExceptionConstant.error_occur);
            return ResponseEntity.ok(resMap);
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/delete")
    public ResponseEntity<ResponsePattern> delete(@RequestBody CommandPatternDelete command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            command.setUserId(userEntity.getId());
            command.setDoDeleteEntities(true);
            if (BooleanUtils.isFalse(patternService.delete(CommandGetListPattern.builder()
                    .userId(userEntity.getId())
                    .ids(List.of(command.getId()))
                    .hasEntities(true)
                    .build()))) {
                throw new Exception(ExceptionConstant.error_occur);
            }

            return ResponseEntity.ok(ResponsePattern.builder()
                    .pattern(PatternEntity.builder()
                            .id(command.getId())
                            .userId(command.getUserId())
                            .build())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(patternService.returnException(e.toString(), ResponsePattern.class));
        }
    }

//    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
//    @GetMapping("/get_pagination/by_user_id")
//    public ResponseEntity<Paginated<PatternEntity>> getPaginationByUserId(@RequestParam int page, @RequestParam int size) {
//        try {
//            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))) {
//                throw new Exception("auth_invalid");
//            }
//
//            page--;
//            Paginated<PatternEntity> patterns = patternService.getPagination(userEntity.getId(), page, size);
//            patterns.setPageNumber(++page);
//            return ResponseEntity.ok(patterns);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.ok(new Paginated<>(new ArrayList<>(), 0, 0, 0));
//        }
//    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/get_pagination")
    public ResponseEntity<Paginated<PatternEntity>> getPaginationByUserId(@RequestBody CommandGetListPattern command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))) {
                throw new Exception("auth_invalid");
            }
            command.setUserId(userEntity.getId());
            command.setHasIntentName(true);
            Paginated<PatternEntity> patterns = patternService.getPaginatedList(command, PatternEntity.class, CommandGetListPattern.class);
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new Paginated<>(new ArrayList<>(), 0, 0, 0));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_pagination/by_intent_id/{intentId}")
    public ResponseEntity<Paginated<PatternEntity>> getPaginationByIntentId(@RequestParam int page, @RequestParam int size, @PathVariable String intentId) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))) {
                throw new Exception("auth_invalid");
            }

//            patternService.getPaginatedList(CommandGetListPattern.builder()
//                            .userId(userEntity.getId())
//                            .intentId(intentId)
//                            .page(page)
//                            .size(size)
//                    .build(), PatternEntity.class, CommandGetListPattern.class);

//            page--;
//            Paginated<PatternEntity> patterns = patternService.getPaginationByIntentId(intent_id, page, size);
//            patterns.setPageNumber(++page);

            return ResponseEntity.ok(patternService.getPaginatedList(CommandGetListPattern.builder()
                    .userId(userEntity.getId())
                    .intentId(intentId)
                    .page(page)
                    .size(size)
                    .build(), PatternEntity.class, CommandGetListPattern.class));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new Paginated<>(new ArrayList<>(), 0, 0, 0));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/import/excel")
    public ResponseEntity<Document> importFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))) {
                throw new Exception("auth_invalid");
            }

            if (StringUtils.isBlank(file.getOriginalFilename()) || file.getOriginalFilename().split("\\.").length < 2) {
                throw new Exception("invalid_file");
            }

            // Check extension type
            String extensionType = file.getOriginalFilename().split("\\.")[file.getOriginalFilename().split("\\.").length - 1];
            if (!Arrays.asList("xls", "xlsx", "csv").contains(extensionType)) {
                throw new Exception("only_accept_xlsx_or_xls_or_csv");
            }

            String sessionId = UUID.randomUUID().toString();
            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(sessionId + "." + extensionType);
                fos.write(file.getBytes());
            } catch (Throwable e) {
                e.printStackTrace();
                log.error(e.getLocalizedMessage());
            } finally {
                try {
                    if (fos != null) fos.close();
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage());
                }
            }

            CompletableFuture.runAsync(() -> {
                final String finalUserId = userEntity.getId();
                try {
                    patternService.importFromFile(CommandImportPatternsFromFile.builder()
                            .userId(finalUserId)
                            .sessionId(sessionId)
                            .extensionType(extensionType)
                            .build());
                } catch (Exception e) {
                    log.error(e.getMessage());
                    File importFile = new File(sessionId + "." + extensionType);
                    if (importFile.exists()) importFile.delete();
                }
            });

            Document document = new Document();
            document.put("session_id", sessionId);
            document.put("http_status", "OK");
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            Document resMap = new Document();
            resMap.put("http_status", "EXPECTATION_FAILED");
            resMap.put("exception_code", StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ExceptionConstant.error_occur);
            return ResponseEntity.ok(resMap);
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/import/excel/status")
    public ResponseEntity<Document> getStatusImportExcel(@RequestParam String sessionId) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))) {
                throw new Exception("auth_invalid");
            }
            String importExcelJedisKey = Constant.JedisPrefix.userIdPrefix_ + userEntity.getId() +
                    Constant.JedisPrefix.COLON +
                    Constant.JedisPrefix.Pattern.importExcelSessionIdPrefix_ + sessionId;
            String responseStatusStr = jedisService.get(importExcelJedisKey);
            if (StringUtils.isBlank(responseStatusStr)) {
                throw new Exception("status_null");
            }
            Document resMap = objectMapper.readValue(responseStatusStr, Document.class);
            resMap.put("http_status", "OK");
            return ResponseEntity.ok(resMap);
        } catch (Exception e) {
            Document resMap = new Document();
            resMap.put("http_status", "EXPECTATION_FAILED");
            resMap.put("exception_code", StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ExceptionConstant.error_occur);
            return ResponseEntity.ok(resMap);
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/import/excel/get_template")
    public ResponseEntity<Document> getImportExcelTemplate() {
        try {
            Document resMap = new Document();
            resMap.put("link", Constant.Pattern.importExcelTemplateDownloadLink);
            resMap.put("http_status", "OK");
            return ResponseEntity.ok(resMap);
        } catch (Exception e) {
            Document resMap = new Document();
            resMap.put("http_status", "EXPECTATION_FAILED");
            resMap.put("exception_code", StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ExceptionConstant.error_occur);
            return ResponseEntity.ok(resMap);
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/export/excel")
    public ResponseEntity<Document> exportExcel(@RequestBody CommandGetListPattern command) {
        try {
            Document resMap = new Document();
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))) {
                throw new Exception("auth_invalid");
            }
            command.setUserId(userEntity.getId());
            String sessionId = UUID.randomUUID().toString();
            CompletableFuture.runAsync(() -> {
                try {
                    patternService.exportExcel(command, sessionId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            resMap.put("session_id", sessionId);
            resMap.put("http_status", "OK");
            return ResponseEntity.ok(resMap);
        } catch (Exception e) {
            Document resMap = new Document();
            resMap.put("http_status", "EXPECTATION_FAILED");
            resMap.put("exception_code", StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ExceptionConstant.error_occur);
            return ResponseEntity.ok(resMap);
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/export/excel/get_file/{fileName}")
    public ResponseEntity<Document> exportExcelGetFile(@PathVariable String fileName) {
        try {
            if (!fileName.contains(Constant.Pattern.exportExcelFileNamePrefix)) {
                throw new Exception("file_invalid");
            }

            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))) {
                throw new Exception("auth_invalid");
            }
            String userId = userEntity.getId();
            Document resMap = new Document();
            Path path = Paths.get(Constant.fileDataPath + userId + "/" + fileName);
            byte[] data = Files.readAllBytes(path);

            resMap.put("base64", Base64.getEncoder().encodeToString(data));
            resMap.put("http_status", "OK");
            return ResponseEntity.ok(resMap);
        } catch (Exception e) {
            Document resMap = new Document();
            resMap.put("http_status", "EXPECTATION_FAILED");
            resMap.put("exception_code", ExceptionConstant.error_occur);
            return ResponseEntity.ok(resMap);
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/export/excel/status")
    public ResponseEntity<Document> getStatusExportExcel(@RequestParam String sessionId) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))) {
                throw new Exception("auth_invalid");
            }
            String importExcelJedisKey = Constant.JedisPrefix.userIdPrefix_ + userEntity.getId() +
                    Constant.JedisPrefix.COLON +
                    Constant.JedisPrefix.Pattern.exportExcelSessionIdPrefix_ + sessionId;
            String responseStatusStr = jedisService.get(importExcelJedisKey);
            if (StringUtils.isBlank(responseStatusStr)) {
                throw new Exception("status_null");
            }
            Document resMap = objectMapper.readValue(responseStatusStr, Document.class);
            resMap.put("http_status", "OK");
            return ResponseEntity.ok(resMap);
        } catch (Exception e) {
            Document resMap = new Document();
            resMap.put("http_status", "EXPECTATION_FAILED");
            resMap.put("exception_code", StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ExceptionConstant.error_occur);
            return ResponseEntity.ok(resMap);
        }
    }
}

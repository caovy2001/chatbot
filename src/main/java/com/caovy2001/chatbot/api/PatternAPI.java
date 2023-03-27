package com.caovy2001.chatbot.api;

import com.caovy2001.chatbot.constant.Constant;
import com.caovy2001.chatbot.constant.ExceptionConstant;
import com.caovy2001.chatbot.entity.PatternEntity;
import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.BaseService;
import com.caovy2001.chatbot.service.jedis.IJedisService;
import com.caovy2001.chatbot.service.pattern.IPatternService;
import com.caovy2001.chatbot.service.pattern.command.*;
import com.caovy2001.chatbot.service.pattern.response.ResponsePattern;
import com.caovy2001.chatbot.service.pattern.response.ResponsePatternAdd;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
@RequestMapping("/pattern")
public class PatternAPI {
    @Autowired
    private BaseService baseService;

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
            ResponsePatternAdd responsePatternAdd = patternService.add(command);
            return ResponseEntity.ok(responsePatternAdd);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePatternAdd.class));
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
            ResponsePattern responsePattern = patternService.update(command);
            return ResponseEntity.ok(responsePattern);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePattern.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_all/by_intent_id/{intentId}")
    public ResponseEntity<ResponsePattern> getByIntentId(@PathVariable String intentId) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            ResponsePattern patterns = patternService.getByIntentId(intentId, userEntity.getId());
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePattern.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_all/by_user_id")
    public ResponseEntity<ResponsePattern> getByUserId() {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            ResponsePattern patterns = patternService.getByUserId(userEntity.getId());
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePattern.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get/{id}")
    public ResponseEntity<ResponsePattern> getById(@PathVariable String id) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank(userEntity.getId()))
                throw new Exception("auth_invalid");

            ResponsePattern patterns = patternService.getById(id, userEntity.getId());
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePattern.class));
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
            ResponsePattern responsePattern = patternService.delete(command);
            return ResponseEntity.ok(responsePattern);
        } catch (Exception e) {
            return ResponseEntity.ok(baseService.returnException(e.toString(), ResponsePattern.class));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_pagination/by_user_id")
    public ResponseEntity<Paginated<PatternEntity>> getPaginationByUserId(@RequestParam int page, @RequestParam int size) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))) {
                throw new Exception("auth_invalid");
            }

            page--;
            Paginated<PatternEntity> patterns = patternService.getPaginationByUserId(userEntity.getId(), page, size);
            patterns.setPageNumber(++page);
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new Paginated<>(new ArrayList<>(), 0, 0, 0));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/get_pagination")
    public ResponseEntity<Paginated<PatternEntity>> getPaginationByUserId(@RequestBody CommandGetListPattern command) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))) {
                throw new Exception("auth_invalid");
            }
            command.setUserId(userEntity.getId());
            Paginated<PatternEntity> patterns = patternService.getPaginationByUserId(command);
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new Paginated<>(new ArrayList<>(), 0, 0, 0));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @GetMapping("/get_pagination/by_intent_id/{intent_id}")
    public ResponseEntity<Paginated<PatternEntity>> getPaginationByIntentId(@RequestParam int page, @RequestParam int size, @PathVariable String intent_id) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))) {
                throw new Exception("auth_invalid");
            }

            page--;
            Paginated<PatternEntity> patterns = patternService.getPaginationByIntentId(intent_id, page, size);
            patterns.setPageNumber(++page);
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new Paginated<>(new ArrayList<>(), 0, 0, 0));
        }
    }

    @PreAuthorize("hasAnyAuthority('ALLOW_ACCESS')")
    @PostMapping("/import/excel")
    public ResponseEntity<Document> importFromExcel(@RequestParam("excel_file") MultipartFile excelFile) {
        try {
            UserEntity userEntity = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (userEntity == null || StringUtils.isBlank((userEntity.getId()))) {
                throw new Exception("auth_invalid");
            }

            if (StringUtils.isBlank(excelFile.getOriginalFilename()) || excelFile.getOriginalFilename().split("\\.").length < 2) {
                throw new Exception("invalid_file");
            }

            if (!Arrays.asList("xls", "xlsx").contains(excelFile.getOriginalFilename().split("\\.")[1])) {
                throw new Exception("only_accept_xlsx");
            }

            String sessionId = UUID.randomUUID().toString();
            FileOutputStream fos = null;

            try {
                String path_file = sessionId + ".xlsx";
                fos = new FileOutputStream(path_file);
                byte[] data = excelFile.getBytes();
                fos.write(data);
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
                try {
                    patternService.importFromExcel(CommandImportPatternsFromExcel.builder()
                            .userId(userEntity.getId())
                            .sessionId(sessionId)
                            .build());
                } catch (Exception e) {
                    log.info(e.getMessage());
                    File importFile = new File(sessionId + ".xlsx");
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
}

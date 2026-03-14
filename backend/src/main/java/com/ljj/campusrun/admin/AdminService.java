package com.ljj.campusrun.admin;

import com.ljj.campusrun.activity.ActivityService;
import com.ljj.campusrun.activity.CreateActivityRequest;
import com.ljj.campusrun.activity.CreateTutorialRequest;
import com.ljj.campusrun.domain.entity.Badge;
import com.ljj.campusrun.domain.entity.FeedPost;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.domain.enums.ReviewStatus;
import com.ljj.campusrun.domain.enums.UserRole;
import com.ljj.campusrun.domain.enums.UserStatus;
import com.ljj.campusrun.repository.AuditLogRepository;
import com.ljj.campusrun.repository.BadgeRepository;
import com.ljj.campusrun.repository.FeedPostRepository;
import com.ljj.campusrun.repository.UserRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final FeedPostRepository feedPostRepository;
    private final BadgeRepository badgeRepository;
    private final AuditLogRepository auditLogRepository;
    private final ActivityService activityService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        var students = userRepository.findByRole(UserRole.STUDENT);
        double totalDistance = students.stream().mapToDouble(User::getTotalDistanceKm).sum();
        int totalPoints = students.stream().mapToInt(User::getPoints).sum();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentCount", students.size());
        data.put("totalDistanceKm", totalDistance);
        data.put("totalPoints", totalPoints);
        data.put("pendingPosts", feedPostRepository.findByReviewStatusOrderByCreatedAtDesc(ReviewStatus.PENDING).size());
        data.put("latestLogs", auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream().limit(8).map(this::mapLog).toList());
        return data;
    }

    @Transactional(readOnly = true)
    public Object listStudents() {
        return userRepository.findByRole(UserRole.STUDENT).stream()
                .map(this::mapStudent)
                .toList();
    }

    @Transactional(readOnly = true)
    public Object getStudent(Long studentId) {
        return userRepository.findById(studentId)
                .map(this::mapStudent)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
    }

    @Transactional
    public Object importStudents(Long adminUserId, MultipartFile file) {
        User operator = getAdmin(adminUserId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传学生导入文件");
        }
        List<StudentImportRow> rows = parseImportFile(file);
        int created = 0;
        int updated = 0;
        for (StudentImportRow row : rows) {
            User student = userRepository.findByStudentNo(row.studentNo())
                    .or(() -> userRepository.findByUsername(row.username()))
                    .orElseGet(User::new);
            boolean isNew = student.getId() == null;
            if (!isNew && student.getRole() != UserRole.STUDENT) {
                throw new IllegalArgumentException("导入文件中包含非学生账号：" + row.studentNo());
            }
            student.setUsername(row.username());
            student.setStudentNo(row.studentNo());
            student.setDisplayName(row.displayName());
            student.setCollege(row.college());
            student.setClassName(row.className());
            student.setRole(UserRole.STUDENT);
            student.setStatus(row.status());
            if (row.password() != null && !row.password().isBlank()) {
                student.setPassword(passwordEncoder.encode(row.password()));
            } else if (isNew) {
                throw new IllegalArgumentException("新导入学生必须提供初始密码：" + row.studentNo());
            }
            userRepository.save(student);
            if (isNew) {
                created++;
            } else {
                updated++;
            }
        }
        auditLogService.log(operator, "IMPORT_STUDENTS", "sys_user", null,
                "created=" + created + ",updated=" + updated);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", rows.size());
        data.put("created", created);
        data.put("updated", updated);
        return data;
    }

    @Transactional
    public Object updateStudentStatus(Long adminUserId, Long studentId, UpdateStudentStatusRequest request) {
        User operator = getAdmin(adminUserId);
        User student = getStudentEntity(studentId);
        student.setStatus(parseStatus(request.status()));
        User saved = userRepository.save(student);
        auditLogService.log(operator, "UPDATE_STUDENT_STATUS", "sys_user", String.valueOf(saved.getId()), saved.getStatus().name());
        return mapStudent(saved);
    }

    @Transactional
    public Object resetStudentBinding(Long adminUserId, Long studentId) {
        User operator = getAdmin(adminUserId);
        User student = getStudentEntity(studentId);
        student.setWechatOpenid(null);
        student.setWechatUnionid(null);
        student.setWechatBoundAt(null);
        User saved = userRepository.save(student);
        auditLogService.log(operator, "RESET_STUDENT_BINDING", "sys_user", String.valueOf(saved.getId()), "重置微信绑定");
        return mapStudent(saved);
    }

    @Transactional(readOnly = true)
    public Object listPendingPosts() {
        return feedPostRepository.findByReviewStatusOrderByCreatedAtDesc(ReviewStatus.PENDING).stream()
                .map(this::mapPendingPost)
                .toList();
    }

    @Transactional
    public FeedPost reviewPost(Long adminUserId, Long postId, ReviewPostRequest request) {
        User operator = userRepository.findById(adminUserId).orElseThrow(() -> new IllegalArgumentException("管理员不存在"));
        FeedPost post = feedPostRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("动态不存在"));
        switch (request.action().toUpperCase()) {
            case "APPROVE" -> post.setReviewStatus(ReviewStatus.APPROVED);
            case "REJECT" -> post.setReviewStatus(ReviewStatus.REJECTED);
            case "OFFLINE" -> post.setReviewStatus(ReviewStatus.OFFLINE);
            case "FEATURE" -> post.setFeatured(true);
            default -> throw new IllegalArgumentException("不支持的审核动作");
        }
        FeedPost saved = feedPostRepository.save(post);
        auditLogService.log(operator, "REVIEW_POST", "feed_post", String.valueOf(postId), request.action() + ":" + request.remark());
        return saved;
    }

    @Transactional
    public Badge saveBadge(Long adminUserId, CreateBadgeRequest request) {
        User operator = userRepository.findById(adminUserId).orElseThrow(() -> new IllegalArgumentException("管理员不存在"));
        Badge badge = new Badge();
        badge.setCode(request.code());
        badge.setName(request.name());
        badge.setDescription(request.description());
        badge.setIcon(request.icon());
        badge.setRuleType(request.ruleType());
        badge.setRuleThreshold(request.ruleThreshold());
        Badge saved = badgeRepository.save(badge);
        auditLogService.log(operator, "SAVE_BADGE", "badge", String.valueOf(saved.getId()), saved.getName());
        return saved;
    }

    @Transactional
    public Object saveActivity(Long adminUserId, CreateActivityRequest request) {
        User operator = userRepository.findById(adminUserId).orElseThrow(() -> new IllegalArgumentException("管理员不存在"));
        var saved = activityService.saveActivity(request);
        auditLogService.log(operator, "SAVE_ACTIVITY", "activity", String.valueOf(saved.getId()), saved.getTitle());
        return saved;
    }

    @Transactional
    public Object saveTutorial(Long adminUserId, CreateTutorialRequest request) {
        User operator = userRepository.findById(adminUserId).orElseThrow(() -> new IllegalArgumentException("管理员不存在"));
        var saved = activityService.saveTutorial(request);
        auditLogService.log(operator, "SAVE_TUTORIAL", "tutorial", String.valueOf(saved.getId()), saved.getTitle());
        return saved;
    }

    @Transactional(readOnly = true)
    public Object listBadges() {
        return badgeRepository.findByActiveTrueOrderByRuleThresholdAsc();
    }

    @Transactional(readOnly = true)
    public Object listLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::mapLog)
                .toList();
    }

    private Map<String, Object> mapStudent(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("displayName", user.getDisplayName());
        data.put("studentNo", user.getStudentNo() == null ? "" : user.getStudentNo());
        data.put("college", user.getCollege() == null ? "" : user.getCollege());
        data.put("className", user.getClassName() == null ? "" : user.getClassName());
        data.put("bio", user.getBio() == null ? "" : user.getBio());
        data.put("totalDistanceKm", user.getTotalDistanceKm());
        data.put("totalDurationSeconds", user.getTotalDurationSeconds());
        data.put("points", user.getPoints());
        data.put("levelValue", user.getLevelValue());
        data.put("status", user.getStatus());
        data.put("wechatBound", user.getWechatOpenid() != null && !user.getWechatOpenid().isBlank());
        data.put("wechatBoundAt", user.getWechatBoundAt());
        return data;
    }

    private Map<String, Object> mapPendingPost(FeedPost post) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", post.getId());
        data.put("content", post.getContent());
        data.put("riskTags", post.getRiskTags() == null ? "" : post.getRiskTags());
        data.put("reportCount", post.getReportCount());
        data.put("reviewStatus", post.getReviewStatus());
        data.put("createdAt", post.getCreatedAt());
        data.put("user", mapUserSummary(post.getUser()));
        return data;
    }

    private Map<String, Object> mapLog(com.ljj.campusrun.domain.entity.AuditLog log) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", log.getId());
        data.put("action", log.getAction());
        data.put("targetType", log.getTargetType());
        data.put("targetId", log.getTargetId() == null ? "" : log.getTargetId());
        data.put("detail", log.getDetail() == null ? "" : log.getDetail());
        data.put("createdAt", log.getCreatedAt());
        return data;
    }

    private Map<String, Object> mapUserSummary(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("displayName", user.getDisplayName());
        return data;
    }

    private User getAdmin(Long adminUserId) {
        User operator = userRepository.findById(adminUserId).orElseThrow(() -> new IllegalArgumentException("管理员不存在"));
        if (operator.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("仅管理员可执行该操作");
        }
        return operator;
    }

    private User getStudentEntity(Long studentId) {
        User student = userRepository.findById(studentId).orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("目标账号不是学生");
        }
        return student;
    }

    private List<StudentImportRow> parseImportFile(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (filename.endsWith(".xlsx")) {
                return parseXlsx(file);
            }
            if (filename.endsWith(".csv")) {
                return parseCsv(file);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("学生导入文件解析失败");
        }
        throw new IllegalArgumentException("仅支持 .csv 或 .xlsx 格式导入");
    }

    private List<StudentImportRow> parseCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("导入文件为空");
            }
            String[] headers = splitCsvLine(headerLine);
            List<StudentImportRow> rows = new java.util.ArrayList<>();
            String line;
            int rowIndex = 1;
            while ((line = reader.readLine()) != null) {
                rowIndex++;
                if (line.isBlank()) {
                    continue;
                }
                rows.add(mapImportRow(headers, splitCsvLine(line), rowIndex));
            }
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("导入文件中没有有效学生数据");
            }
            return rows;
        }
    }

    private List<StudentImportRow> parseXlsx(MultipartFile file) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            var sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() <= 1) {
                throw new IllegalArgumentException("导入文件中没有有效学生数据");
            }
            String[] headers = rowToArray(sheet.getRow(0));
            List<StudentImportRow> rows = new java.util.ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String[] values = rowToArray(row);
                boolean empty = true;
                for (String value : values) {
                    if (value != null && !value.isBlank()) {
                        empty = false;
                        break;
                    }
                }
                if (!empty) {
                    rows.add(mapImportRow(headers, values, i + 1));
                }
            }
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("导入文件中没有有效学生数据");
            }
            return rows;
        }
    }

    private String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }

    private String[] rowToArray(Row row) {
        int lastCellNum = Math.max(row.getLastCellNum(), 0);
        String[] values = new String[lastCellNum];
        for (int i = 0; i < lastCellNum; i++) {
            Cell cell = row.getCell(i);
            values[i] = cell == null ? "" : cell.toString().trim();
        }
        return values;
    }

    private StudentImportRow mapImportRow(String[] headers, String[] values, int rowIndex) {
        Map<String, String> data = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i] == null ? "" : headers[i].trim().toLowerCase();
            data.put(header, i < values.length ? values[i].trim() : "");
        }
        String studentNo = firstNonBlank(data, "student_no", "studentno", "学号");
        String username = firstNonBlank(data, "username", "用户名");
        String displayName = firstNonBlank(data, "display_name", "displayname", "姓名");
        String college = firstNonBlank(data, "college", "学院");
        String className = firstNonBlank(data, "class_name", "classname", "班级");
        String password = firstNonBlank(data, "password", "初始密码");
        String status = firstNonBlank(data, "status", "状态");

        if (studentNo == null || displayName == null || college == null || className == null) {
            throw new IllegalArgumentException("第 " + rowIndex + " 行学生信息不完整");
        }
        String finalUsername = username == null ? studentNo : username;
        return new StudentImportRow(
                studentNo,
                finalUsername,
                displayName,
                college,
                className,
                password,
                status == null ? UserStatus.ACTIVE : parseStatus(status)
        );
    }

    private String firstNonBlank(Map<String, String> data, String... keys) {
        for (String key : keys) {
            String value = data.get(key.toLowerCase());
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("学生状态不合法，仅支持 ACTIVE 或 DISABLED");
        }
    }

    private record StudentImportRow(
            String studentNo,
            String username,
            String displayName,
            String college,
            String className,
            String password,
            UserStatus status
    ) {
    }
}

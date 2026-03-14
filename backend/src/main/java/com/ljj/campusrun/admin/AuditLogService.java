package com.ljj.campusrun.admin;

import com.ljj.campusrun.domain.entity.AuditLog;
import com.ljj.campusrun.domain.entity.User;
import com.ljj.campusrun.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(User operator, String action, String targetType, String targetId, String detail) {
        AuditLog log = new AuditLog();
        log.setOperator(operator);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }
}

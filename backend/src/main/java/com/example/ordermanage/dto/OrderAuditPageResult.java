package com.example.ordermanage.dto;

import java.util.List;

public record OrderAuditPageResult(long total, List<OrderAuditListItem> list, long pendingCount) {
}

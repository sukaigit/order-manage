# Backend Smoke Checklist (Slice 9)

`mvn -q test` → EXIT=0 · Tests run: 171 (164 prior + 7 new)

## Export endpoints

- [x] GET /api/orders/export — OrderExportTest (200, Content-Disposition, application/vnd.ms-excel, POI rows == list total)
- [x] GET /api/suppliers/export — ExportMiscTest
- [x] GET /api/reports/export — ExportMiscTest (admin); non-admin → 403

## Operation log

- [x] Login inserts tb_operation_log row (action=登录) — OperationLogWriterTest
- [x] GET /api/logs finds new 登录 row
- [x] SchemaSmokeTest seed logs=5 preserved (order.oplog.enabled=false in test resources; enabled in OperationLogWriterTest with cleanup)

## Regression

- [x] Full suite green including pre-existing 164 tests

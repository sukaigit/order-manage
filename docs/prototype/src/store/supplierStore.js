// 共享供应商数据，供应商管理/订单/报表共用
const suppliers = [
  {code:'SUP-001',name:'华东原料供应商',contact:'张三',phone:'13800000001',address:'上海市浦东新区',active:true,date:'2026-01-01 00:00:00',remark:''},
  {code:'SUP-002',name:'南方包装供应商',contact:'李四',phone:'13800000002',address:'广州市天河区',active:true,date:'2026-01-01 00:00:00',remark:''},
  {code:'SUP-003',name:'北方物流供应商',contact:'王五',phone:'13800000003',address:'北京市朝阳区',active:false,date:'2026-01-01 00:00:00',remark:''},
]

export function getSuppliers() { return suppliers }
export function addSupplier(s) { suppliers.push(s) }
export function updateSupplier(code, data) { const i=suppliers.findIndex(x=>x.code===code); if(i>=0) Object.assign(suppliers[i], data) }
export function deleteSupplier(code) { const i=suppliers.findIndex(x=>x.code===code); if(i>=0) suppliers.splice(i,1) }
export function supplierName(code) { const s=suppliers.find(x=>x.code===code); return s?s.name:code }

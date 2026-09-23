// 共享订单数据，订单列表/审核列表/报表共用
const orders = [
  {code:'ORD20260801',name:'华东原料月度采购单',amount:12500.00,supplier:'SUP-001',status:'待审核',date:'2026-08-03',auditUser:'',auditTime:'',opinion:'',remark:''},
  {code:'ORD20260802',name:'南方包装耗材采购单',amount:5600.00,supplier:'SUP-002',status:'待审核',date:'2026-08-10',auditUser:'',auditTime:'',opinion:'',remark:''},
  {code:'ORD20260703',name:'原料紧急补货单',amount:8800.00,supplier:'SUP-001',status:'已驳回',date:'2026-07-15',auditUser:'审核员',auditTime:'2026-07-16 10:20:00',opinion:'单价与合同不符，请核对后重新提交',remark:''},
  {code:'ORD20260704',name:'包装纸箱批量采购单',amount:3200.00,supplier:'SUP-002',status:'已完成',date:'2026-07-08',auditUser:'审核员',auditTime:'2026-07-09 14:30:00',opinion:'',remark:''},
  {code:'ORD20260905',name:'华东原料季度大单',amount:26000.00,supplier:'SUP-001',status:'已完成',date:'2026-09-01',auditUser:'审核员',auditTime:'2026-09-02 09:15:00',opinion:'',remark:''},
  {code:'ORD20260606',name:'包装膜采购单',amount:1500.00,supplier:'SUP-002',status:'已完成',date:'2026-06-12',auditUser:'审核员',auditTime:'2026-06-13 11:00:00',opinion:'',remark:''},
  {code:'ORD20260807',name:'原料小批量试单',amount:2400.00,supplier:'SUP-001',status:'待审核',date:'2026-08-22',auditUser:'',auditTime:'',opinion:'',remark:''},
  {code:'ORD20260708',name:'包装定制采购单',amount:6700.00,supplier:'SUP-002',status:'已驳回',date:'2026-07-25',auditUser:'审核员',auditTime:'2026-07-26 16:40:00',opinion:'请补充供应商报价单附件',remark:''},
  {code:'ORD20260909',name:'九月包装追加单',amount:4100.00,supplier:'SUP-002',status:'待审核',date:'2026-09-10',auditUser:'',auditTime:'',opinion:'',remark:''},
  {code:'ORD20260610',name:'原料半年度结算单',amount:18900.00,supplier:'SUP-001',status:'已完成',date:'2026-06-28',auditUser:'审核员',auditTime:'2026-06-29 15:20:00',opinion:'',remark:''},
]

export function getOrders() { return orders }
export function addOrder(o) { orders.push(o) }
export function updateOrder(code, data) { const i=orders.findIndex(x=>x.code===code); if(i>=0) Object.assign(orders[i], data) }
export function deleteOrder(code) { const i=orders.findIndex(x=>x.code===code); if(i>=0) orders.splice(i,1) }
export function isSupplierReferenced(code) { return orders.some(o=>o.supplier===code) }

// 共享部门数据，部门管理和用户管理共用同一个数据源
const depts = [
  {code:'DEPT-000',name:'总部',remark:'默认根部门'},
]

export function getDepts() { return depts }
export function addDept(d) { depts.push(d) }
export function updateDept(code, data) { const i=depts.findIndex(x=>x.code===code); if(i>=0) Object.assign(depts[i], data) }
export function deleteDept(code) { const i=depts.findIndex(x=>x.code===code); if(i>=0) depts.splice(i,1) }

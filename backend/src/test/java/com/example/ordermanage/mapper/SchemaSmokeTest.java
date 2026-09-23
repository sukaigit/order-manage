package com.example.ordermanage.mapper;

import com.example.ordermanage.entity.Menu;
import com.example.ordermanage.entity.OperationLog;
import com.example.ordermanage.entity.Role;
import com.example.ordermanage.entity.Supplier;
import com.example.ordermanage.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class SchemaSmokeTest {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private MenuMapper menuMapper;
    @Autowired
    private FunctionMapper functionMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OperationLogMapper operationLogMapper;
    @Autowired
    private SupplierMapper supplierMapper;

    @Test
    void seedCountsMatchInitSql() {
        assertEquals(3L, roleMapper.selectCount(null));
        assertEquals(17L, menuMapper.selectCount(null));
        assertEquals(32L, functionMapper.selectCount(null));
        assertEquals(1L, userMapper.selectCount(null));
        assertEquals(5L, operationLogMapper.selectCount(null));
        assertEquals(3L, supplierMapper.selectCount(null));
    }
}

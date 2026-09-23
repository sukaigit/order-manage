package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.annotation.OpLog;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.dto.OrgTreeNode;
import com.example.ordermanage.dto.OrganizationSaveRequest;
import com.example.ordermanage.entity.Organization;
import com.example.ordermanage.mapper.OrganizationMapper;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrganizationService {

    private static final String[] LEVEL_CHAIN = {"hq", "branch1", "branch2", "sub1", "sub2"};

    private final OrganizationMapper organizationMapper;

    public OrganizationService(OrganizationMapper organizationMapper) {
        this.organizationMapper = organizationMapper;
    }

    public PageResult<OrgTreeNode> tree(String code, String name, String shortName, String level,
                                         PageParam p) {
        p.validate();
        List<Organization> all = organizationMapper.selectList(
                new QueryWrapper<Organization>().orderByAsc("id"));
        Map<Long, Organization> byId = all.stream()
                .collect(Collectors.toMap(Organization::getId, o -> o));
        Map<Long, List<Organization>> childrenOf = new HashMap<>();
        for (Organization o : all) {
            if (o.getParentId() != null) {
                childrenOf.computeIfAbsent(o.getParentId(), k -> new ArrayList<>()).add(o);
            }
        }
        for (List<Organization> list : childrenOf.values()) {
            list.sort(Comparator.comparing(Organization::getId));
        }

        Set<Long> matched = new HashSet<>();
        boolean hasFilter = StringUtils.hasText(code) || StringUtils.hasText(name)
                || StringUtils.hasText(shortName) || StringUtils.hasText(level);
        for (Organization o : all) {
            if (!hasFilter || matches(o, code, name, shortName, level)) {
                matched.add(o.getId());
            }
        }
        if (matched.isEmpty()) {
            return new PageResult<>(0, List.of());
        }

        Set<Long> kept = new HashSet<>(matched);
        for (Long id : matched) {
            Long cur = id;
            while (cur != null && byId.containsKey(cur)) {
                kept.add(cur);
                cur = byId.get(cur).getParentId();
            }
        }
        Deque<Long> queue = new ArrayDeque<>(matched);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            for (Organization child : childrenOf.getOrDefault(id, List.of())) {
                if (kept.add(child.getId())) {
                    queue.add(child.getId());
                }
            }
        }

        List<Long> rootIds = all.stream()
                .filter(o -> o.getParentId() == null && kept.contains(o.getId()))
                .map(Organization::getId)
                .sorted()
                .collect(Collectors.toList());

        long total = rootIds.size();
        int from = (int) ((p.getPage() - 1) * p.getPageSize());
        if (from >= rootIds.size()) {
            return new PageResult<>(total, List.of());
        }
        int to = Math.min(from + p.getPageSize(), rootIds.size());
        List<OrgTreeNode> pageRoots = new ArrayList<>();
        for (Long rootId : rootIds.subList(from, to)) {
            pageRoots.add(buildNode(byId.get(rootId), childrenOf, kept));
        }
        return new PageResult<>(total, pageRoots);
    }

    private boolean matches(Organization o, String code, String name, String shortName, String level) {
        if (StringUtils.hasText(code) && !contains(o.getCode(), code)) {
            return false;
        }
        if (StringUtils.hasText(name) && !contains(o.getName(), name)) {
            return false;
        }
        if (StringUtils.hasText(shortName) && !contains(o.getShortName(), shortName)) {
            return false;
        }
        if (StringUtils.hasText(level) && !level.equals(o.getLevel())) {
            return false;
        }
        return true;
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword.trim());
    }

    private OrgTreeNode buildNode(Organization org, Map<Long, List<Organization>> childrenOf,
                                  Set<Long> kept) {
        OrgTreeNode node = new OrgTreeNode();
        node.setId(org.getId());
        node.setCode(org.getCode());
        node.setName(org.getName());
        node.setShortName(org.getShortName());
        node.setLevel(org.getLevel());
        node.setParentId(org.getParentId());
        node.setContact(org.getContact());
        node.setPhone(org.getPhone());
        node.setRegion(org.getRegion());
        node.setAddress(org.getAddress());
        node.setRemark(org.getRemark());
        node.setCreateTime(org.getCreateTime());
        node.setUpdateTime(org.getUpdateTime());
        for (Organization child : childrenOf.getOrDefault(org.getId(), List.of())) {
            if (kept.contains(child.getId())) {
                node.getChildren().add(buildNode(child, childrenOf, kept));
            }
        }
        return node;
    }

    @OpLog(action = "新增机构", targetSpEL = "#root.result.code")
    public OrgTreeNode create(OrganizationSaveRequest req) {
        if (!StringUtils.hasText(req.getCode())) {
            throw new BizException(Err.BAD_REQUEST, "请输入机构编号");
        }
        if (!StringUtils.hasText(req.getName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入机构名称");
        }
        if (!StringUtils.hasText(req.getShortName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入机构简称");
        }
        if (!StringUtils.hasText(req.getRegion())) {
            throw new BizException(Err.BAD_REQUEST, "请输入所在地区");
        }
        if (!StringUtils.hasText(req.getAddress())) {
            throw new BizException(Err.BAD_REQUEST, "请输入详细地址");
        }
        String code = req.getCode().trim();
        if (!code.matches("^[A-Za-z0-9]+$")) {
            throw new BizException(Err.UNPROCESSABLE, Err.ORG_CODE_INVALID);
        }
        if (organizationMapper.selectCount(new QueryWrapper<Organization>().eq("code", code)) > 0) {
            throw new BizException(Err.CONFLICT, Err.ORG_CODE_EXISTS);
        }
        String level;
        if (req.getParentId() == null) {
            level = "hq";
        } else {
            Organization parent = organizationMapper.selectById(req.getParentId());
            if (parent == null) {
                throw new BizException(Err.BAD_REQUEST, "上级机构不存在");
            }
            level = nextLevel(parent.getLevel());
            if (level == null) {
                throw new BizException(Err.UNPROCESSABLE, Err.ORG_LAST_LEVEL);
            }
        }
        Organization org = new Organization();
        org.setCode(code);
        org.setName(req.getName().trim());
        org.setShortName(req.getShortName().trim());
        org.setLevel(level);
        org.setParentId(req.getParentId());
        org.setContact(req.getContact());
        org.setPhone(req.getPhone());
        org.setRegion(req.getRegion().trim());
        org.setAddress(req.getAddress().trim());
        org.setRemark(req.getRemark());
        org.setCreateTime(LocalDateTime.now());
        org.setUpdateTime(LocalDateTime.now());
        organizationMapper.insert(org);
        return toNode(org);
    }

    @OpLog(action = "编辑机构", targetSpEL = "#root.args[1].code")
    public OrgTreeNode update(Long id, OrganizationSaveRequest req) {
        Organization org = require(id);
        if (!StringUtils.hasText(req.getName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入机构名称");
        }
        if (!StringUtils.hasText(req.getShortName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入机构简称");
        }
        if (!StringUtils.hasText(req.getRegion())) {
            throw new BizException(Err.BAD_REQUEST, "请输入所在地区");
        }
        if (!StringUtils.hasText(req.getAddress())) {
            throw new BizException(Err.BAD_REQUEST, "请输入详细地址");
        }
        org.setName(req.getName().trim());
        org.setShortName(req.getShortName().trim());
        org.setContact(req.getContact());
        org.setPhone(req.getPhone());
        org.setRegion(req.getRegion().trim());
        org.setAddress(req.getAddress().trim());
        org.setRemark(req.getRemark());
        org.setUpdateTime(LocalDateTime.now());
        organizationMapper.updateById(org);
        return toNode(org);
    }

    @OpLog(action = "删除机构", targetSpEL = "#root.args[0]")
    public void delete(Long id) {
        Organization org = require(id);
        Long children = organizationMapper.selectCount(
                new QueryWrapper<Organization>().eq("parent_id", id));
        if (children > 0) {
            throw new BizException(Err.UNPROCESSABLE, Err.ORG_HAS_CHILDREN);
        }
        organizationMapper.deleteById(org.getId());
    }

    public Organization require(Long id) {
        Organization org = organizationMapper.selectById(id);
        if (org == null) {
            throw new BizException(Err.NOT_FOUND, "机构不存在");
        }
        return org;
    }

    private String nextLevel(String level) {
        for (int i = 0; i < LEVEL_CHAIN.length; i++) {
            if (LEVEL_CHAIN[i].equals(level)) {
                return i + 1 < LEVEL_CHAIN.length ? LEVEL_CHAIN[i + 1] : null;
            }
        }
        return null;
    }

    private OrgTreeNode toNode(Organization org) {
        OrgTreeNode node = new OrgTreeNode();
        node.setId(org.getId());
        node.setCode(org.getCode());
        node.setName(org.getName());
        node.setShortName(org.getShortName());
        node.setLevel(org.getLevel());
        node.setParentId(org.getParentId());
        node.setContact(org.getContact());
        node.setPhone(org.getPhone());
        node.setRegion(org.getRegion());
        node.setAddress(org.getAddress());
        node.setRemark(org.getRemark());
        node.setCreateTime(org.getCreateTime());
        node.setUpdateTime(org.getUpdateTime());
        return node;
    }
}

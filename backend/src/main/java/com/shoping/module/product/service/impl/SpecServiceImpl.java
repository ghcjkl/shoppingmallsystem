package com.shoping.module.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.module.product.entity.SpecGroup;
import com.shoping.module.product.entity.SpecOption;
import com.shoping.module.product.entity.SkuSpecOption;
import com.shoping.module.product.mapper.SpecGroupMapper;
import com.shoping.module.product.mapper.SpecOptionMapper;
import com.shoping.module.product.mapper.SkuSpecOptionMapper;
import com.shoping.module.product.service.SpecService;
import com.shoping.module.product.vo.SpecGroupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SpecServiceImpl implements SpecService {

    private final SpecGroupMapper specGroupMapper;
    private final SpecOptionMapper specOptionMapper;
    private final SkuSpecOptionMapper skuSpecOptionMapper;

    @Override
    public List<SpecGroupVO> listGroups(Integer productType) {
        List<SpecGroup> groups = specGroupMapper.selectList(new LambdaQueryWrapper<SpecGroup>()
                .and(w -> w.eq(SpecGroup::getProductType, productType).or().isNull(SpecGroup::getProductType))
                .orderByAsc(SpecGroup::getSort));
        // 已被 SKU 关联的选项所属组（如「杯型」）属于 SKU 维度，不应作为下单时的制作选项重复选择
        Set<Long> skuOptionIds = new HashSet<>();
        skuSpecOptionMapper.selectList(null)
                .forEach(link -> skuOptionIds.add(link.getSpecOptionId()));
        return groups.stream().map(group -> {
            List<SpecOption> options = specOptionMapper.selectList(new LambdaQueryWrapper<SpecOption>()
                    .eq(SpecOption::getGroupId, group.getId())
                    .orderByAsc(SpecOption::getSort));
            boolean skuGroup = options.stream().anyMatch(o -> skuOptionIds.contains(o.getId()));
            return SpecGroupVO.from(group, options, skuGroup);
        }).toList();
    }
}

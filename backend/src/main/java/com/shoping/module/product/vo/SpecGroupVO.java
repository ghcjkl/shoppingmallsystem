package com.shoping.module.product.vo;

import com.shoping.module.product.entity.SpecGroup;
import com.shoping.module.product.entity.SpecOption;
import lombok.Data;

import java.util.List;

@Data
public class SpecGroupVO {

    private Long id;
    private String name;
    private Integer productType;
    private Integer sort;
    /** 是否为 SKU 维度组（如「杯型」决定 SKU/价格/库存），true 时应由规格选择决定，不作为制作选项展示 */
    private Boolean skuGroup;
    private List<SpecOption> options;

    public static SpecGroupVO from(SpecGroup group, List<SpecOption> options) {
        return from(group, options, false);
    }

    public static SpecGroupVO from(SpecGroup group, List<SpecOption> options, boolean skuGroup) {
        SpecGroupVO vo = new SpecGroupVO();
        vo.setId(group.getId());
        vo.setName(group.getName());
        vo.setProductType(group.getProductType());
        vo.setSort(group.getSort());
        vo.setSkuGroup(skuGroup);
        vo.setOptions(options);
        return vo;
    }
}

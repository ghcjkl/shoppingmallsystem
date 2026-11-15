package com.shoping.module.product.service;

import com.shoping.module.product.vo.SpecGroupVO;

import java.util.List;

public interface SpecService {

    List<SpecGroupVO> listGroups(Integer productType);
}

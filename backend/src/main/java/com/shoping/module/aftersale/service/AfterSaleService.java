package com.shoping.module.aftersale.service;

import com.shoping.module.aftersale.dto.AfterSaleApplyRequest;
import com.shoping.module.aftersale.dto.AfterSaleProcessRequest;
import com.shoping.module.aftersale.entity.AfterSale;

import java.util.List;

public interface AfterSaleService {

    AfterSale apply(AfterSaleApplyRequest request);

    List<AfterSale> listByUser(Long userId);

    List<AfterSale> listAll(Integer status);

    AfterSale process(AfterSaleProcessRequest request);
}

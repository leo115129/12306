package com.example.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.example.train.business.domain.*;
import com.example.train.business.enums.SeatColEnum;
import com.example.train.business.mapper.DailyTrainCarriageMapper;
import com.example.train.business.req.DailyTrainCarriageQueryReq;
import com.example.train.business.req.DailyTrainCarriageSaveReq;
import com.example.train.business.resp.DailyTrainCarriageQueryResp;
import com.example.train.common.resp.PageResp;
import com.example.train.common.util.SnowUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DailyTrainCarriageService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainCarriageService.class);

    @Resource
    private DailyTrainCarriageMapper dailyTrainCarriageMapper;

    @Resource
    private TrainCarriageService trainCarriageService;

    public void save(DailyTrainCarriageSaveReq req) {
        DateTime now = DateTime.now();

        List<SeatColEnum> seatColEnums = SeatColEnum.getColsByType(req.getSeatType());
        req.setColCount(seatColEnums.size());
        req.setSeatCount(req.getColCount() * req.getRowCount());

        DailyTrainCarriage dailyTrainCarriage = BeanUtil.copyProperties(req, DailyTrainCarriage.class);


        if (ObjectUtil.isNull(dailyTrainCarriage.getId())) {
            dailyTrainCarriage.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainCarriage.setCreateTime(now);
            dailyTrainCarriage.setUpdateTime(now);
            dailyTrainCarriageMapper.insert(dailyTrainCarriage);
        } else {
            dailyTrainCarriage.setUpdateTime(now);
            dailyTrainCarriageMapper.updateByPrimaryKey(dailyTrainCarriage);
        }
    }

    public PageResp<DailyTrainCarriageQueryResp> queryList(DailyTrainCarriageQueryReq req) {
        DailyTrainCarriageExample dailyTrainCarriageExample = new DailyTrainCarriageExample();
        dailyTrainCarriageExample.setOrderByClause("date desc,train_code asc,'index' asc");
        DailyTrainCarriageExample.Criteria criteria = dailyTrainCarriageExample.createCriteria();

        if(ObjectUtil.isNotEmpty(req.getDate())){
            criteria.andDateEqualTo(req.getDate());
        }
        if(ObjectUtil.isNotEmpty(req.getCode())){
            criteria.andTrainCodeEqualTo(req.getCode());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainCarriage> dailyTrainCarriageList = dailyTrainCarriageMapper.selectByExample(dailyTrainCarriageExample);

        PageInfo<DailyTrainCarriage> pageInfo = new PageInfo<>(dailyTrainCarriageList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainCarriageQueryResp> list = BeanUtil.copyToList(dailyTrainCarriageList, DailyTrainCarriageQueryResp.class);

        PageResp<DailyTrainCarriageQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainCarriageMapper.deleteByPrimaryKey(id);
    }

    public void genDaily(Date date, String code){
        //删除车次已有数据
        DailyTrainCarriageExample dailyTrainCarriageExample=new DailyTrainCarriageExample();
        dailyTrainCarriageExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(code);
        dailyTrainCarriageMapper.deleteByExample(dailyTrainCarriageExample);

        List<TrainCarriage> trainCarriages = trainCarriageService.selectByTrainCode(code);
        if(CollUtil.isEmpty(trainCarriages)){
            LOG.info("该车次没有车厢基础数据，生成该车次车站信息结束");
            return;
        }

        for (TrainCarriage trainCarriage : trainCarriages) {
            DateTime now=DateTime.now();
            DailyTrainCarriage dailyTrainCarriage=BeanUtil.copyProperties(trainCarriage,DailyTrainCarriage.class);
            dailyTrainCarriage.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainCarriage.setCreateTime(now);
            dailyTrainCarriage.setUpdateTime(now);
            dailyTrainCarriage.setDate(date);
            dailyTrainCarriageMapper.insert(dailyTrainCarriage);
        }
    }

    public List<DailyTrainCarriage> selectBySeatType(Date date,String code,String seatType){
        DailyTrainCarriageExample dailyTrainCarriageExample=new DailyTrainCarriageExample();
        dailyTrainCarriageExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(code)
                .andSeatTypeEqualTo(seatType);
        return dailyTrainCarriageMapper.selectByExample(dailyTrainCarriageExample);

    }
}

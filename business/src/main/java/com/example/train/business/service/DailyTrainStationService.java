package com.example.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.example.train.business.domain.DailyTrainStation;
import com.example.train.business.domain.DailyTrainStationExample;
import com.example.train.business.domain.TrainStation;
import com.example.train.business.mapper.DailyTrainStationMapper;
import com.example.train.business.req.DailyTrainStationQueryReq;
import com.example.train.business.req.DailyTrainStationSaveReq;
import com.example.train.business.resp.DailyTrainStationQueryResp;
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
public class DailyTrainStationService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainStationService.class);

    @Resource
    private DailyTrainStationMapper dailyTrainStationMapper;
    @Resource
    private TrainStationService trainStationService;

    public void save(DailyTrainStationSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrainStation dailyTrainStation = BeanUtil.copyProperties(req, DailyTrainStation.class);
        if (ObjectUtil.isNull(dailyTrainStation.getId())) {
            dailyTrainStation.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainStation.setCreateTime(now);
            dailyTrainStation.setUpdateTime(now);
            dailyTrainStationMapper.insert(dailyTrainStation);
        } else {
            dailyTrainStation.setUpdateTime(now);
            dailyTrainStationMapper.updateByPrimaryKey(dailyTrainStation);
        }
    }

    public PageResp<DailyTrainStationQueryResp> queryList(DailyTrainStationQueryReq req) {
        DailyTrainStationExample dailyTrainStationExample = new DailyTrainStationExample();
        dailyTrainStationExample.setOrderByClause("date desc,train_code asc,'index' asc");
        DailyTrainStationExample.Criteria criteria = dailyTrainStationExample.createCriteria();

        if(ObjectUtil.isNotEmpty(req.getDate())){
            criteria.andDateEqualTo(req.getDate());
        }
        if(ObjectUtil.isNotEmpty(req.getCode())){
            criteria.andTrainCodeEqualTo(req.getCode());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainStation> dailyTrainStationList = dailyTrainStationMapper.selectByExample(dailyTrainStationExample);

        PageInfo<DailyTrainStation> pageInfo = new PageInfo<>(dailyTrainStationList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainStationQueryResp> list = BeanUtil.copyToList(dailyTrainStationList, DailyTrainStationQueryResp.class);

        PageResp<DailyTrainStationQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainStationMapper.deleteByPrimaryKey(id);
    }

    public void genDaily(Date date, String code){
        //删除车次已有数据
        DailyTrainStationExample dailyTrainStationExample=new DailyTrainStationExample();
        dailyTrainStationExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(code);
        dailyTrainStationMapper.deleteByExample(dailyTrainStationExample);

        List<TrainStation> trainStations = trainStationService.selectByTrainCode(code);
        if(CollUtil.isEmpty(trainStations)){
            LOG.info("该车次没有车站基础数据，生成改车次车站信息结束");
            return;
        }

        for (TrainStation trainStation : trainStations) {
            DateTime now=DateTime.now();
            DailyTrainStation dailyTrainStation=BeanUtil.copyProperties(trainStation,DailyTrainStation.class);
            dailyTrainStation.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainStation.setCreateTime(now);
            dailyTrainStation.setUpdateTime(now);
            dailyTrainStation.setDate(date);
            dailyTrainStationMapper.insert(dailyTrainStation);
        }
    }

    /**
     * 按车次日期查询车站列表，用于界面显示一列车经过的车站
     */
    public List<DailyTrainStationQueryResp> queryByTrain(Date date,String trainCode) {
        DailyTrainStationExample dailyTrainStationExample = new DailyTrainStationExample();
        dailyTrainStationExample.setOrderByClause("`index` asc");
        dailyTrainStationExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
        List<DailyTrainStation> list = dailyTrainStationMapper.selectByExample(dailyTrainStationExample);
        return BeanUtil.copyToList(list, DailyTrainStationQueryResp.class);
    }

    public long countByTrainCode(Date date, String trainCode) {
        DailyTrainStationExample dailyTrainStationExample=new DailyTrainStationExample();
        dailyTrainStationExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
        long l = dailyTrainStationMapper.countByExample(dailyTrainStationExample);
        return l;
    }
}

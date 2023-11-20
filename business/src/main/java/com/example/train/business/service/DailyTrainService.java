package com.example.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.example.train.business.domain.DailyTrain;
import com.example.train.business.domain.DailyTrainExample;
import com.example.train.business.domain.Train;
import com.example.train.business.mapper.DailyTrainMapper;
import com.example.train.business.req.DailyTrainQueryReq;
import com.example.train.business.req.DailyTrainSaveReq;
import com.example.train.business.resp.DailyTrainQueryResp;
import com.example.train.common.resp.PageResp;
import com.example.train.common.util.SnowUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class DailyTrainService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainService.class);

    @Resource
    private DailyTrainMapper dailyTrainMapper;

    @Resource
    private TrainService trainService;

    @Resource
    private DailyTrainStationService dailyTrainStationService;

    @Resource
    private DailyTrainCarriageService dailyTrainCarriageService;

    @Resource
    private DailyTrainSeatService dailyTrainSeatService;

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    @Resource
    private SkTokenService skTokenService;

    public void save(DailyTrainSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrain dailyTrain = BeanUtil.copyProperties(req, DailyTrain.class);
        if (ObjectUtil.isNull(dailyTrain.getId())) {
            dailyTrain.setId(SnowUtil.getSnowflakeNextId());
            dailyTrain.setCreateTime(now);
            dailyTrain.setUpdateTime(now);
            dailyTrainMapper.insert(dailyTrain);
        } else {
            dailyTrain.setUpdateTime(now);
            dailyTrainMapper.updateByPrimaryKey(dailyTrain);
        }
    }

    public PageResp<DailyTrainQueryResp> queryList(DailyTrainQueryReq req) {
        DailyTrainExample dailyTrainExample = new DailyTrainExample();
        dailyTrainExample.setOrderByClause("'date' desc,code asc");
        DailyTrainExample.Criteria criteria = dailyTrainExample.createCriteria();

        if(ObjectUtil.isNotEmpty(req.getDate())){
            criteria.andDateEqualTo(req.getDate());
        }
        if(ObjectUtil.isNotEmpty(req.getCode())){
            criteria.andCodeEqualTo(req.getCode());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrain> dailyTrainList = dailyTrainMapper.selectByExample(dailyTrainExample);

        PageInfo<DailyTrain> pageInfo = new PageInfo<>(dailyTrainList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainQueryResp> list = BeanUtil.copyToList(dailyTrainList, DailyTrainQueryResp.class);

        PageResp<DailyTrainQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainMapper.deleteByPrimaryKey(id);
    }

    /**
     * 生成每日所有车次信息，包括车次、车厢、车站、座位
     * @param date
     */
    public void genDaily(Date date) {
        List<Train> trains = trainService.selectAll();
        if(CollUtil.isEmpty(trains)){
            LOG.info("没有车次基础数据，任务结束");
            return;
        }
        for (Train train : trains) {
            genDailyTrain(date, train);

        }
    }

    @Transactional
    public void genDailyTrain(Date date, Train train) {
        //删除车次已有数据
        DailyTrainExample dailyTrainExample=new DailyTrainExample();
        dailyTrainExample.createCriteria().andDateEqualTo(date).andCodeEqualTo(train.getCode());
        dailyTrainMapper.deleteByExample(dailyTrainExample);

        //生成该车次数据
        DateTime now=DateTime.now();
        DailyTrain dailyTrain=BeanUtil.copyProperties(train,DailyTrain.class);
        dailyTrain.setId(SnowUtil.getSnowflakeNextId());
        dailyTrain.setCreateTime(now);
        dailyTrain.setUpdateTime(now);
        dailyTrain.setDate(date);
        dailyTrainMapper.insert(dailyTrain);

        //生成该车次车站数据
        dailyTrainStationService.genDaily(date,train.getCode());

        //生成该车次车厢数据
        dailyTrainCarriageService.genDaily(date,train.getCode());

        //生成该车次座位数据
        dailyTrainSeatService.genDaily(date,train.getCode());

        //生成该车次余票数据
        dailyTrainTicketService.genDaily(dailyTrain,date,train.getCode());

        //生成令牌余量数据
        skTokenService.genDaily(date,train.getCode());

    }
}

package com.example.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.train.business.domain.DailyTrainSeat;
import com.example.train.business.domain.DailyTrainSeatExample;
import com.example.train.business.domain.TrainSeat;
import com.example.train.business.domain.TrainStation;
import com.example.train.business.mapper.DailyTrainSeatMapper;
import com.example.train.business.req.DailyTrainSeatQueryReq;
import com.example.train.business.req.DailyTrainSeatSaveReq;
import com.example.train.business.req.SeatSellReq;
import com.example.train.business.resp.DailyTrainSeatQueryResp;
import com.example.train.business.resp.SeatSellResp;
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
public class DailyTrainSeatService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainSeatService.class);

    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;

    @Resource
    private  TrainSeatService trainSeatService;

    @Resource
    private TrainStationService trainStationService;

    public void save(DailyTrainSeatSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrainSeat dailyTrainSeat = BeanUtil.copyProperties(req, DailyTrainSeat.class);
        if (ObjectUtil.isNull(dailyTrainSeat.getId())) {
            dailyTrainSeat.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainSeat.setCreateTime(now);
            dailyTrainSeat.setUpdateTime(now);
            dailyTrainSeatMapper.insert(dailyTrainSeat);
        } else {
            dailyTrainSeat.setUpdateTime(now);
            dailyTrainSeatMapper.updateByPrimaryKey(dailyTrainSeat);
        }
    }

    public PageResp<DailyTrainSeatQueryResp> queryList(DailyTrainSeatQueryReq req) {
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.setOrderByClause("train_code asc,carriage_index asc,carriage_seat_index asc");
        DailyTrainSeatExample.Criteria criteria = dailyTrainSeatExample.createCriteria();

        if(ObjectUtil.isNotEmpty(req.getDate())){
            criteria.andDateEqualTo(req.getDate());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainSeat> dailyTrainSeatList = dailyTrainSeatMapper.selectByExample(dailyTrainSeatExample);

        PageInfo<DailyTrainSeat> pageInfo = new PageInfo<>(dailyTrainSeatList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainSeatQueryResp> list = BeanUtil.copyToList(dailyTrainSeatList, DailyTrainSeatQueryResp.class);

        PageResp<DailyTrainSeatQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainSeatMapper.deleteByPrimaryKey(id);
    }

    public void genDaily(Date date, String code){
        //删除车次已有数据
        DailyTrainSeatExample dailyTrainSeatExample=new DailyTrainSeatExample();
        dailyTrainSeatExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(code);
        dailyTrainSeatMapper.deleteByExample(dailyTrainSeatExample);

        List<TrainStation> trainStations = trainStationService.selectByTrainCode(code);
        String sell= StrUtil.fillBefore("",'0',trainStations.size()-1);

        List<TrainSeat> trainSeats = trainSeatService.selectByTrainCode(code);
        if(CollUtil.isEmpty(trainSeats)){
            LOG.info("该车次没有车厢基础数据，生成该车次车站信息结束");
            return;
        }

        for (TrainSeat trainSeat : trainSeats) {
            DateTime now=DateTime.now();
            DailyTrainSeat dailyTrainSeat=BeanUtil.copyProperties(trainSeat,DailyTrainSeat.class);
            dailyTrainSeat.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainSeat.setCreateTime(now);
            dailyTrainSeat.setUpdateTime(now);
            dailyTrainSeat.setDate(date);
            dailyTrainSeat.setSell(sell);
            dailyTrainSeatMapper.insert(dailyTrainSeat);
        }

    }

    public Integer countSeat(String code,Date date){
        return countSeat(code,date,null);
    }

    public int countSeat(String code, Date date, String seatType){
        DailyTrainSeatExample dailyTrainSeatExample=new DailyTrainSeatExample();
        DailyTrainSeatExample.Criteria criteria=dailyTrainSeatExample.createCriteria();
        criteria.andTrainCodeEqualTo(code).andDateEqualTo(date);
        if(StrUtil.isNotBlank(seatType)){
            criteria.andSeatTypeEqualTo(seatType);
        }
        long l = Math.toIntExact(dailyTrainSeatMapper.countByExample(dailyTrainSeatExample));
        if(l==0L){
            return -1;
        }
        return (int)l;
    }

    public List<DailyTrainSeat> selectByCarriage(Date date,String code,Integer carriageIndex){
        DailyTrainSeatExample dailyTrainSeatExample=new DailyTrainSeatExample();
        dailyTrainSeatExample.setOrderByClause("carriage_seat_index asc");
        dailyTrainSeatExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(code)
                .andCarriageIndexEqualTo(carriageIndex);
        return dailyTrainSeatMapper.selectByExample(dailyTrainSeatExample);
    }

    /**
     * 查询某日某车次的所有座位
     */
    public List<SeatSellResp> querySeatSell(SeatSellReq req) {
        Date date = req.getDate();
        String trainCode = req.getTrainCode();
        LOG.info("查询日期【{}】车次【{}】的座位销售信息", DateUtil.formatDate(date), trainCode);
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.setOrderByClause("`carriage_index` asc, carriage_seat_index asc");
        dailyTrainSeatExample.createCriteria()
                .andDateEqualTo(date)
                .andTrainCodeEqualTo(trainCode);
        return BeanUtil.copyToList(dailyTrainSeatMapper.selectByExample(dailyTrainSeatExample), SeatSellResp.class);
    }
}

package com.example.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.train.business.domain.DailyTrain;
import com.example.train.business.domain.DailyTrainTicket;
import com.example.train.business.domain.DailyTrainTicketExample;
import com.example.train.business.domain.TrainStation;
import com.example.train.business.enums.SeatTypeEnum;
import com.example.train.business.enums.TrainTypeEnum;
import com.example.train.business.mapper.DailyTrainTicketMapper;
import com.example.train.business.req.DailyTrainTicketQueryReq;
import com.example.train.business.req.DailyTrainTicketSaveReq;
import com.example.train.business.resp.DailyTrainTicketQueryResp;
import com.example.train.common.resp.PageResp;
import com.example.train.common.util.SnowUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class DailyTrainTicketService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainTicketService.class);

    @Resource
    private DailyTrainTicketMapper dailyTrainTicketMapper;

    @Resource
    private TrainStationService trainStationService;

    @Resource
    private DailyTrainSeatService dailyTrainSeatService;

    public void save(DailyTrainTicketSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrainTicket dailyTrainTicket = BeanUtil.copyProperties(req, DailyTrainTicket.class);
        if (ObjectUtil.isNull(dailyTrainTicket.getId())) {
            dailyTrainTicket.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainTicket.setCreateTime(now);
            dailyTrainTicket.setUpdateTime(now);
            dailyTrainTicketMapper.insert(dailyTrainTicket);
        } else {
            dailyTrainTicket.setUpdateTime(now);
            dailyTrainTicketMapper.updateByPrimaryKey(dailyTrainTicket);
        }
    }

    public PageResp<DailyTrainTicketQueryResp> queryList(DailyTrainTicketQueryReq req) {
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.setOrderByClause("id desc");
        DailyTrainTicketExample.Criteria criteria = dailyTrainTicketExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainTicket> dailyTrainTicketList = dailyTrainTicketMapper.selectByExample(dailyTrainTicketExample);

        PageInfo<DailyTrainTicket> pageInfo = new PageInfo<>(dailyTrainTicketList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainTicketQueryResp> list = BeanUtil.copyToList(dailyTrainTicketList, DailyTrainTicketQueryResp.class);

        PageResp<DailyTrainTicketQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainTicketMapper.deleteByPrimaryKey(id);
    }

    @Transactional
    public void genDaily(DailyTrain dailyTrain,Date date, String code){
        //删除某日某车次余票已有数据
        DailyTrainTicketExample dailyTrainTicketExample=new DailyTrainTicketExample();
        dailyTrainTicketExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(code);
        dailyTrainTicketMapper.deleteByExample(dailyTrainTicketExample);

        //查询途径的所有车站
        List<TrainStation> trainStations = trainStationService.selectByTrainCode(code);
        if(CollUtil.isEmpty(trainStations)){
            LOG.info("该车次没有车站数据，生成余票信息结束");
            return;
        }

        DateTime now=DateTime.now();
        for(int i=0;i<trainStations.size();++i){
            //得到出发站
            TrainStation trainStationStart = trainStations.get(i);
            BigDecimal sumKm=BigDecimal.ZERO;
            for(int j=i+1;j<trainStations.size();++j){
                TrainStation trainStationEnd = trainStations.get(j);
                sumKm=sumKm.add(trainStationEnd.getKm());
                DailyTrainTicket dailyTrainTicket=new DailyTrainTicket();
                dailyTrainTicket.setId(SnowUtil.getSnowflakeNextId());
                dailyTrainTicket.setDate(date);
                dailyTrainTicket.setTrainCode(code);
                dailyTrainTicket.setStart(trainStationStart.getName());
                dailyTrainTicket.setStartPinyin(trainStationStart.getNamePinyin());
                dailyTrainTicket.setStartTime(trainStationStart.getOutTime());
                dailyTrainTicket.setStartIndex(trainStationStart.getIndex());
                dailyTrainTicket.setEnd(trainStationEnd.getName());
                dailyTrainTicket.setEndPinyin(trainStationEnd.getNamePinyin());
                dailyTrainTicket.setEndTime(trainStationEnd.getInTime());
                dailyTrainTicket.setEndIndex(trainStationEnd.getIndex());
                Integer Ydz= dailyTrainSeatService.countSeat(code, date, SeatTypeEnum.YDZ.getCode());
                Integer Edz= dailyTrainSeatService.countSeat(code, date, SeatTypeEnum.EDZ.getCode());
                Integer Rw= dailyTrainSeatService.countSeat(code, date, SeatTypeEnum.RW.getCode());
                Integer Yw= dailyTrainSeatService.countSeat(code, date, SeatTypeEnum.YW.getCode());
                dailyTrainTicket.setYdz(Ydz);
                String trainType=dailyTrain.getType();
                BigDecimal fieldBy = EnumUtil.getFieldBy(TrainTypeEnum::getPriceRate, TrainTypeEnum::getCode, trainType);
                dailyTrainTicket.setYdzPrice(sumKm.multiply(SeatTypeEnum.YDZ.getPrice()).multiply(fieldBy).setScale(2, RoundingMode.HALF_UP));
                dailyTrainTicket.setEdz(Edz);
                dailyTrainTicket.setEdzPrice(sumKm.multiply(SeatTypeEnum.EDZ.getPrice()).multiply(fieldBy).setScale(2, RoundingMode.HALF_UP));
                dailyTrainTicket.setRw(Rw);
                dailyTrainTicket.setRwPrice(sumKm.multiply(SeatTypeEnum.RW.getPrice()).multiply(fieldBy).setScale(2, RoundingMode.HALF_UP));
                dailyTrainTicket.setYw(Yw);
                dailyTrainTicket.setYwPrice(sumKm.multiply(SeatTypeEnum.YW.getPrice()).multiply(fieldBy).setScale(2, RoundingMode.HALF_UP));
                dailyTrainTicket.setCreateTime(now);
                dailyTrainTicket.setUpdateTime(now);
                dailyTrainTicketMapper.insert(dailyTrainTicket);
            }
        }
    }

}

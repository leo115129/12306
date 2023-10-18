package com.example.train.member.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.example.train.common.context.LoginMemberContext;
import com.example.train.common.resp.PageResp;
import com.example.train.common.util.SnowUtil;
import com.example.train.member.domain.Passenger;
import com.example.train.member.domain.PassengerExample;
import com.example.train.member.mapper.PassengerMapper;
import com.example.train.member.req.PassengerQueryReq;
import com.example.train.member.req.PassengerSaveReq;
import com.example.train.member.resp.PassengerQueryResp;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PassengerService  {

    Logger Log = LoggerFactory.getLogger(PassengerService.class);

    @Resource
    private PassengerMapper passengerMapper;

    public void save(PassengerSaveReq req){
        DateTime now=DateTime.now();
        Passenger passenger=BeanUtil.copyProperties(req,Passenger.class);
        if(ObjectUtil.isNull(passenger.getId())){
        passenger.setMemberId(LoginMemberContext.getId());
        passenger.setId(SnowUtil.getSnowflakeNextId());
        passenger.setCreateTime(now);
        passenger.setUpdateTime(now);
        passengerMapper.insert(passenger);
        }else{
            passenger.setUpdateTime(now);
            passengerMapper.updateByPrimaryKeySelective(passenger);
        }
    }

    public PageResp<PassengerQueryResp> queryList(PassengerQueryReq req) {
        PassengerExample passengerExample=new PassengerExample();
        PassengerExample.Criteria criteria=passengerExample.createCriteria();
        if(ObjectUtil.isNotNull(req.getMemberId())) {
            criteria.andMemberIdEqualTo(req.getMemberId());
        }
        PageHelper.startPage(req.getPage(),req.getSize());
        List<Passenger> passengerList= passengerMapper.selectByExample(passengerExample);
        PageInfo<Passenger> pageInfo=new PageInfo<>(passengerList);
        Log.info("总行数：{}",pageInfo.getTotal());
        Log.info("总页数：{}",pageInfo.getPages());
        List<PassengerQueryResp> passengerQueryRespList=BeanUtil.copyToList(passengerList, PassengerQueryResp.class);
        PageResp<PassengerQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(pageInfo.getTotal());
        objectPageResp.setList(passengerQueryRespList);
        return objectPageResp;
    }

    public void delete(Long id) {
        passengerMapper.deleteByPrimaryKey(id);
    }
}

package com.example.train.member.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.example.train.common.context.LoginMemberContext;
import com.example.train.common.util.SnowUtil;
import com.example.train.member.domain.Passenger;
import com.example.train.member.domain.PassengerExample;
import com.example.train.member.mapper.PassengerMapper;
import com.example.train.member.req.PassengerQueryReq;
import com.example.train.member.req.PassengerSaveReq;
import com.example.train.member.resp.PassengerQueryResp;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PassengerService {

    @Resource
    private PassengerMapper passengerMapper;

    public void save(PassengerSaveReq req){
        DateTime now=DateTime.now();
        Passenger passenger=BeanUtil.copyProperties(req,Passenger.class);
        passenger.setMemberId(LoginMemberContext.getId());
        passenger.setId(SnowUtil.getSnowflakeNextId());
        passenger.setCreateTime(now);
        passenger.setUpdateTime(now);
        passengerMapper.insert(passenger);
    }

    public List<PassengerQueryResp> queryList(PassengerQueryReq req) {
        PassengerExample passengerExample=new PassengerExample();
        PassengerExample.Criteria criteria=passengerExample.createCriteria();
        if(ObjectUtil.isNotNull(req.getMemberId())) {
            criteria.andMemberIdEqualTo(req.getMemberId());
        }
        List<Passenger> passengerList= passengerMapper.selectByExample(passengerExample);
        return BeanUtil.copyToList(passengerList, PassengerQueryResp.class);
    }
}

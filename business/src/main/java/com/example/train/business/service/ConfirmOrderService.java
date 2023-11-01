package com.example.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.example.train.business.domain.*;
import com.example.train.business.enums.ConfirmOrderStatusEnum;
import com.example.train.business.enums.SeatColEnum;
import com.example.train.business.enums.SeatTypeEnum;
import com.example.train.business.mapper.ConfirmOrderMapper;
import com.example.train.business.req.ConfirmOrderDoReq;
import com.example.train.business.req.ConfirmOrderQueryReq;
import com.example.train.business.req.ConfirmOrderTicketReq;
import com.example.train.business.resp.ConfirmOrderQueryResp;
import com.example.train.common.context.LoginMemberContext;
import com.example.train.common.exception.BusinessException;
import com.example.train.common.exception.BusinessExceptionEnum;
import com.example.train.common.resp.PageResp;
import com.example.train.common.util.SnowUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    @Resource
    private DailyTrainCarriageService dailyTrainCarriageService;

    @Resource
    private DailyTrainSeatService dailyTrainSeatService;

    public void save(ConfirmOrderDoReq req) {
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if (ObjectUtil.isNull(confirmOrder.getId())) {
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.insert(confirmOrder);
        } else {
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.updateByPrimaryKey(confirmOrder);
        }
    }

    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req) {
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.setOrderByClause("id desc");
        ConfirmOrderExample.Criteria criteria = confirmOrderExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExample(confirmOrderExample);

        PageInfo<ConfirmOrder> pageInfo = new PageInfo<>(confirmOrderList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<ConfirmOrderQueryResp> list = BeanUtil.copyToList(confirmOrderList, ConfirmOrderQueryResp.class);

        PageResp<ConfirmOrderQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        confirmOrderMapper.deleteByPrimaryKey(id);
    }

    public void doConfirm(ConfirmOrderDoReq req){
        //保存确认订单表、状态初始化
        DateTime now=DateTime.now();
        ConfirmOrder confirmOrder=new ConfirmOrder();
        confirmOrder.setId(SnowUtil.getSnowflakeNextId());
        confirmOrder.setMemberId(LoginMemberContext.getId());
        confirmOrder.setDate(req.getDate());
        confirmOrder.setTrainCode(req.getTrainCode());
        confirmOrder.setStart(req.getStart());
        confirmOrder.setEnd(req.getEnd());
        confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
        confirmOrder.setCreateTime(now);
        confirmOrder.setUpdateTime(now);
        confirmOrder.setTickets(JSON.toJSONString(req.getTickets()));
        confirmOrderMapper.insert(confirmOrder);

        //查出余票记录、需要得到真实的库存
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(req.getDate(), req.getTrainCode(), req.getStart(), req.getEnd());

        //预扣减余票数量、并判断余票是否足够
        reduceTickets(req, dailyTrainTicket);

        //计算座位偏移量
        List<ConfirmOrderTicketReq> tickets = req.getTickets();
        ConfirmOrderTicketReq confirmOrderTicketReq = tickets.get(0);
        if(StrUtil.isBlank(confirmOrderTicketReq.getSeat())){
            LOG.info("本次购票有选座");
            List<SeatColEnum> colsByType = SeatColEnum.getColsByType(confirmOrderTicketReq.getSeatTypeCode());

            //生成用于和前端一样参考的座位
            List<String> referSeatList=new ArrayList<>();
            for(int i=1;i<=2;++i){
                for (SeatColEnum seatColEnum : colsByType) {
                    referSeatList.add(seatColEnum.getCode()+i);
                }
            }

            List<Integer> indexList=new ArrayList<>();
            for (ConfirmOrderTicketReq ticket : tickets) {
                int i = referSeatList.indexOf(ticket.getSeat());
                indexList.add(i);
            }

            List<Integer> offestList=new ArrayList<>();
            for (Integer integer : indexList) {
                Integer offesr=integer-indexList.get(0);
                offestList.add(offesr);
            }

            getSeat(req.getDate(),req.getTrainCode(),confirmOrderTicketReq.getSeatTypeCode(),
            confirmOrderTicketReq.getSeat().split("")[0],offestList);


        }else{
            LOG.info("本次购票没有选座");
            for (ConfirmOrderTicketReq ticket : tickets) {
                getSeat(req.getDate(),req.getTrainCode(),confirmOrderTicketReq.getSeatTypeCode()
                ,null,null);
            }
        }

        //选座
            //

    }

    private void getSeat(Date date, String code, String seatType,String column,List<Integer> offsetList){
        List<DailyTrainCarriage> dailyTrainCarriages = dailyTrainCarriageService.selectBySeatType(date, code, seatType);
        LOG.info("查出符合条件和车厢:{}",dailyTrainCarriages.size());

        //一个车厢一个车厢的获取座位数据
        for (DailyTrainCarriage dailyTrainCarriage : dailyTrainCarriages) {
            List<DailyTrainSeat> dailyTrainSeats = dailyTrainSeatService.selectByCarriage(date, code, dailyTrainCarriage.getIndex());

        }
    }



    private void reduceTickets(ConfirmOrderDoReq req, DailyTrainTicket dailyTrainTicket) {
        for (ConfirmOrderTicketReq ticket : req.getTickets()) {
            String seatTypeCode = ticket.getSeatTypeCode();
            SeatTypeEnum by = EnumUtil.getBy(SeatTypeEnum::getCode, seatTypeCode);
            switch (by){
                case YDZ -> {
                    Integer ydz = dailyTrainTicket.getYdz()-1;
                    if(ydz<0){
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYdz(ydz);
                }
                case EDZ -> {
                    Integer edz = dailyTrainTicket.getEdz()-1;
                    if(edz<0){
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setEdz(edz);
                }
                case YW ->{
                    Integer yw = dailyTrainTicket.getYw()-1;
                    if(yw<0){
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYw(yw);
                }
                case RW -> {
                    Integer rw = dailyTrainTicket.getRw()-1;
                    if(rw<0){
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setRw(rw-1);
                }
            }
        }
    }
}

package com.example.train.business.controller;

import com.example.train.business.req.DailyTrainStationQueryAllReq;
import com.example.train.business.resp.DailyTrainStationQueryResp;
import com.example.train.business.service.DailyTrainStationService;
import com.example.train.common.resp.CommonResp;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/daily-train-station")
public class DailyTrainStationController {

    @Autowired
    private DailyTrainStationService dailyTrainStationService;

    @GetMapping("/query-by-train-code")
    public CommonResp<List<DailyTrainStationQueryResp>> queryByTrain(@Valid DailyTrainStationQueryAllReq req) {
        List<DailyTrainStationQueryResp> list = dailyTrainStationService.queryByTrain(req.getDate(), req.getTrainCode());
        return new CommonResp<>(list);
    }

}

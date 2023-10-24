package com.example.train.business.req;

import com.example.train.common.req.PageReq;

public class TrainSeatQueryReq extends PageReq {

    private String trainCode;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TrainSeatQueryReq{");
        sb.append("trainCode='").append(trainCode).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public String getTrainCode() {
        return trainCode;
    }

    public void setTrainCode(String trainCode) {
        this.trainCode = trainCode;
    }

}

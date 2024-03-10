package com.example.train.business.req;

import com.example.train.common.req.PageReq;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.Objects;

public class DailyTrainTicketQueryReq extends PageReq {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date;

    private String trainCode;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DailyTrainTicketQueryReq{");
        sb.append("date=").append(date);
        sb.append(", trainCode='").append(trainCode).append('\'');
        sb.append(", start='").append(start).append('\'');
        sb.append(", end='").append(end).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTrainCode() {
        return trainCode;
    }

    public void setTrainCode(String trainCode) {
        this.trainCode = trainCode;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    private String start;

    private String end;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyTrainTicketQueryReq that = (DailyTrainTicketQueryReq) o;
        return Objects.equals(date, that.date) && Objects.equals(trainCode, that.trainCode) &&
                Objects.equals(start, that.start) &&
                Objects.equals(end, that.end)
                &&Objects.equals(((DailyTrainTicketQueryReq) o).getPage(),that.getPage())
                && Objects.equals(((DailyTrainTicketQueryReq) o).getSize(),that.getSize());
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, trainCode, start, end,getPage(),getSize());
    }
}

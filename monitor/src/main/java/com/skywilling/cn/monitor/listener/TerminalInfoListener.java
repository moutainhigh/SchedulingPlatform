package com.skywilling.cn.monitor.listener;

import com.alibaba.fastjson.JSONObject;
import com.skywilling.cn.common.enums.TypeField;
import com.skywilling.cn.common.model.BasicCarResponse;
import com.skywilling.cn.manager.car.model.AutonomousCarInfo;
import com.skywilling.cn.manager.car.service.AutoCarInfoService;
import com.skywilling.cn.monitor.model.DTO.TerminalInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * ClassName TerminalInfoListener
 * Author  Lin
 * Date 2019/5/16 21:32
 **/

@Component
public class TerminalInfoListener extends BasicListener {
    @Autowired
    ListenerMap listenerMap;
    @Autowired
    AutoCarInfoService autoCarInfoService;

    @Override
    @PostConstruct
    public void init() {
        listenerMap.addListener(TypeField.TERMINAL_INFO.getDesc(), this);
    }

    @Override
    public BasicCarResponse process(String vin, String body) {
        AutonomousCarInfo car = autoCarInfoService.getOrCreate(vin);
        if (car == null) {
            return null;
        }
        //车端上传的终端信息，包括速度，转角，挡位，姿态（位置，方向），laneId和stationId，时间戳
        TerminalInfo terminalInfo = JSONObject.parseObject(body, TerminalInfo.class);
        car.setVelocity(terminalInfo.getV());
        car.setWheelAngle(terminalInfo.getWheelAngle());
        car.setGear(terminalInfo.getGear());
        car.setPose(terminalInfo.getPose());
        car.setLane(terminalInfo.getLane());
        car.setLane(terminalInfo.getLane());
        car.setStation(terminalInfo.getStation());
        car.setTimestamp(terminalInfo.getTimestamp());
        //异步存入redis
        autoCarInfoService.save(car);
        //然后异步取redis数据判断，不然会变成同步操作，判断调度和处理心跳包同步阻塞

        return new BasicCarResponse(0, new Object());
    }
}

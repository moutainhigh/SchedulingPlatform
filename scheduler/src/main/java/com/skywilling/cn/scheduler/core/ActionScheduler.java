package com.skywilling.cn.scheduler.core;

import com.skywilling.cn.common.exception.park.NoAvailableActionFoundException;
import com.skywilling.cn.common.model.LidarPoint;
import com.skywilling.cn.livemap.model.*;
import com.skywilling.cn.livemap.service.MapService;
import com.skywilling.cn.livemap.service.ShapeMapService;
import com.skywilling.cn.manager.car.model.Action;
import com.skywilling.cn.scheduler.model.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ActionScheduler {

    @Autowired
    ShapeMapService shapeMapService;
    @Autowired
    MapService mapService;

    /**
     * 规划A--》B的路径
     */
    public List<Action> convertToAction(String parkName, Route route) throws NoAvailableActionFoundException {

        LiveMap liveMap=mapService.getMap(parkName);
        if (liveMap == null) return null;

        List<LiveLane> liveLanes = route.getLiveLanes();
        List<Action> actions = new ArrayList<>(liveLanes.size());

        for (LiveLane lane: liveLanes) {
            Action action = this.toAction(shapeMapService.query(parkName, lane.getName()));
            if (action == null) {
                throw new NoAvailableActionFoundException();
            }
            Node from = lane.getFrom();
            Node to = lane.getTo();
            action.setLaneName(lane.getName());
            action.setFrom(from.getName());
            action.setTo(to.getName());
            action.setV(lane.getV());
            actions.add(action);
        }
        return actions;
    }

    /**
     * LaneShapeMap查找LaneShape后，转化为Action
     * @param laneShape
     * @return
     */
    private Action toAction(LaneShape laneShape) {
        List<LidarPoint> lidarPoints = laneShape.getPath();
        Action action = new Action();
        action.setPoints(lidarPoints);
        action.setLaneName(laneShape.getName());
        action.setFrom(laneShape.getFromId());
        action.setTo(laneShape.getToId());
        action.setV(laneShape.getV());
        return action;
    }
}

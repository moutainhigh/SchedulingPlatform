package com.skywilling.cn.scheduler.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.skywilling.cn.common.exception.CarNotAliveException;
import com.skywilling.cn.common.exception.CarNotExistsException;
import com.skywilling.cn.common.exception.IllegalRideException;
import com.skywilling.cn.common.exception.IllegalTaskException;
import com.skywilling.cn.common.exception.park.NoAvailableActionFoundException;
import com.skywilling.cn.livemap.model.LiveLane;
import com.skywilling.cn.livemap.model.LiveStation;
import com.skywilling.cn.livemap.service.StationService;
import com.skywilling.cn.manager.car.model.AutonomousCarInfo;
import com.skywilling.cn.manager.car.service.CarInfoService;
import com.skywilling.cn.scheduler.common.TripStatus;
import com.skywilling.cn.scheduler.core.TripCore;
import com.skywilling.cn.scheduler.model.Route;
import com.skywilling.cn.scheduler.model.Trip;
import com.skywilling.cn.scheduler.repository.TripAccessor;
import com.skywilling.cn.scheduler.service.RouteService;
import com.skywilling.cn.scheduler.service.TripService;
import com.skywilling.cn.scheduler.utils.FunctionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TripServiceImpl implements TripService {
    @Autowired
    TripAccessor tripAccessor;
    @Autowired
    TripCore tripCore;
    @Autowired
    CarInfoService carInfoService;
    @Autowired
    RouteService routeService;
    @Autowired
    StationService stationService;
    //@Autowired
    //private MongoTemplate mongoTemplate;


    @Override
    public boolean stopTrip(String tripId) {
        Trip trip = this.get(tripId);
        if (trip == null || trip.getStatus() > TripStatus.FINISHED.getCode()) {
            return true;
        }
        tripCore.kill(trip);
        return false;
    }

    /**
     * 1.直接获得车辆在哪个车道上
     * 2.规划路径
     */
    @Override
    public String submitTrip(String vin, String parkName, String goal, double velocity, double acceleration,
                             boolean usingDefaultSpeed) throws CarNotExistsException, CarNotAliveException, IllegalRideException {
        AutonomousCarInfo car = carInfoService.getAutoCarInfo(vin);
        if (car == null) {
            throw new CarNotExistsException(vin);
        }
        if (!car.isConnected()) {
            throw new CarNotAliveException(vin);
        }
        //如果车辆已经到达station
        String curStation = car.getStation();
        if (curStation == goal)
            throw new IllegalRideException();
//        LiveStation goalStation = stationService.get(parkName, goal);
//        if (FunctionUtils.distance(goalStation.getX(), goalStation.getY(),
//                car.getLocation().getX(), car.getLocation().getY()) < 1) {
//            throw new IllegalRideException();
//        }
        /**
         * 规划全局路径Route
         */
        Route route = routeService.navigate(parkName, curStation, goal);
        if (!usingDefaultSpeed) {
            for (LiveLane lane : route.getLiveLanes()) {
                lane.setV(velocity);
            }
        }
        /**
         * 生成自动驾驶任务序列
         */
        Trip trip = new Trip(vin, tripCore.generateTripId(vin), route);
        try {
            /**
             * 提交自动任务序列
             */
            tripCore.submitTrip(trip);
            return trip.getId();
        } catch (IllegalTaskException e) {
            e.printStackTrace();
        } catch (NoAvailableActionFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据全局路径重新规划自动驾驶任务序列
     * @param carInfo 车辆信息
     * @param route  全局路径
     * @return
     */
    @Override
    public Trip updateRoute(AutonomousCarInfo carInfo, Route route) {
        Trip trip = this.get(carInfo.getTripId());
        List<LiveLane> old = trip.getRoute().getLiveLanes();
        List<LiveLane> newLanes = new ArrayList<>();
        int i = 0;
        for (LiveLane lane : old) {
            if (!StringUtils.equals(lane.getName(), carInfo.getLane())) {
                newLanes.add(lane);
                i++;
            } else {
                /**
                 * 当前停止点的start序列号
                 */
                trip.setStart(i);
                newLanes.addAll(route.getLiveLanes());
                trip.getRoute().setLiveLanes(newLanes);
                trip.setEnd(trip.getRoute().getLiveLanes().size());
                break;
            }
        }
        tripAccessor.save(trip);
        return trip;
    }

    @Override
    public Trip get(String id) {
        return tripAccessor.find(id);
    }

    @Override
    public Trip getLiveTripBy(String vin) {
        /*Trip lasted = tripAccessor.queryBy(vin);
        if (lasted != null && lasted.getStatus() < RideStatus.FINISHED.getCode()) {
            return lasted;
        }*/
        return null;
    }

    @Override
    public List<Trip> query(int page, int size) {
        return tripAccessor.query(page, size);
    }

    @Override
    public List<Trip> queryBy(String vin, int page, int size) {
        return tripAccessor.queryBy(vin, page, size);
    }

    @Override
    public List<Trip> queryBy(String vin, String start, String end, int page, int size) {
        return tripAccessor.queryBy(vin, start, end, page, size);
    }
}

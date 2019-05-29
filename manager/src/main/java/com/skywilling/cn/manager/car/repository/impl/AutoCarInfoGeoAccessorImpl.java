package com.skywilling.cn.manager.car.repository.impl;


import com.skywilling.cn.manager.car.model.AutonomousCarInfo;
import com.skywilling.cn.manager.car.repository.AutoCarInfoGeoAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AutoCarInfoGeoAccessorImpl implements AutoCarInfoGeoAccessor {

    //private static String COLLECTION_NAME="VEHICLE_GEO";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void save(AutonomousCarInfo autonomousCarInfo) {
        mongoTemplate.save(autonomousCarInfo);
    }

    @Override
    public List<AutonomousCarInfo> getByLane(String laneId) {
        Query query=new Query(Criteria.where("lane").is(laneId));
        return  mongoTemplate.find(query,AutonomousCarInfo.class);
    }

    @Override
    public List<AutonomousCarInfo> findByDist(GeoJsonPoint geoJsonPoint, double dis) {

        Criteria geoCriteria = Criteria.where("position").nearSphere(geoJsonPoint).maxDistance(dis);
        Query query = Query.query(geoCriteria);
        return  mongoTemplate.find(query,AutonomousCarInfo.class);
    }

    @Override
    public AutonomousCarInfo nearVehicle(GeoJsonPoint point) {
        Criteria geoCriteria = Criteria.where("position").nearSphere(point);
        Query query= Query.query(geoCriteria);
        query.with(new PageRequest(0,1));
        return mongoTemplate.find(query,AutonomousCarInfo.class).get(0);
    }

    @Override
    public void remove(String field, String value) {
        mongoTemplate.remove(new Query(Criteria.where(field).is(value)),AutonomousCarInfo.class);
    }

    @Override
    public List<AutonomousCarInfo> getAll() {
        return mongoTemplate.find(new Query(), AutonomousCarInfo.class);
    }

    @Override
    public void insert(AutonomousCarInfo autonomousCarInfo) {
        mongoTemplate.insert(autonomousCarInfo);
    }


}

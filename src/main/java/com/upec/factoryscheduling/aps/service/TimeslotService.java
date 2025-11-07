package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.*;
import com.upec.factoryscheduling.aps.repository.TimeslotRepository;
import com.upec.factoryscheduling.aps.resquest.ProcedureRequest;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TimeslotService {

    private OrderService orderService;
    private WorkCenterService workCenterService;
    private WorkCenterMaintenanceService maintenanceService;
    private TimeslotRepository timeslotRepository;

    @Autowired
    public TimeslotService(OrderService orderService, WorkCenterService workCenterService,
                           WorkCenterMaintenanceService maintenanceService,
                           TimeslotRepository timeslotRepository) {
        this.orderService = orderService;
        this.workCenterService = workCenterService;
        this.maintenanceService = maintenanceService;
        this.timeslotRepository = timeslotRepository;
    }

    @Transactional("h2TransactionManager")
    public Timeslot updateTimeslot(ProcedureRequest request) {
//        Order order = orderService.findFirstByOrderNo(request.getOrderNo());
//        Machine machine = machineService.findFirstByMachineNo(request.getMachineNo());
//        Procedure procedure = procedureService.findFirstByOrderNoAndMachineNoAndProcedureNo(request.getOrderNo(),
//                request.getMachineNo(), request.getProcedureNo());
//        MachineMaintenance maintenance = maintenanceService.findFirstByMachineAndDate(machine, request.getDate().toLocalDate());
//        Timeslot timeslot = timeslotRepository.findFirstByOrderAndProcedureAndMachine(order, procedure,
//                machine);
//        timeslot.setMaintenance(maintenance);
//        timeslot.setManual(Boolean.TRUE);
//        return timeslotRepository.save(timeslot);
        return null;
    }

    public FactorySchedulingSolution findAll() {
        List<Timeslot> timeslots = timeslotRepository.findAll();
        FactorySchedulingSolution solution = new FactorySchedulingSolution();
        solution.setTimeslots(timeslots);
        return solution;
    }


    @Transactional("h2TransactionManager")
    public List<Timeslot> saveAll(List<Timeslot> timeslots) {
        return timeslotRepository.saveAll(timeslots);
    }

    @Transactional("h2TransactionManager")
    public void deleteAll() {
        timeslotRepository.deleteAll();
    }

    @Transactional("h2TransactionManager")
    public List<Timeslot> receiverTimeslot(List<Timeslot> timeslots) {
        Map<String, List<Timeslot>> map = timeslots.stream().collect(Collectors.groupingBy(timeslot -> timeslot.getOrder().getOrderNo()));
        for (List<Timeslot> timeslotList : map.values()) {
            timeslotList = timeslotList.stream().sorted(Comparator.comparingInt(timeslot -> timeslot.getProcedure().getProcedureNo())).collect(Collectors.toList());
            LocalDateTime dateTime = null;
            for (Timeslot timeslot : timeslotList) {
                Order order = timeslot.getOrder();
                if (dateTime == null) {
                    dateTime = order.getFactStartDate();
                }
                Procedure procedure = timeslot.getProcedure();
                procedure.setStartTime(dateTime);
                WorkCenter workCenter = timeslot.getProcedure().getWorkCenterId();
                WorkCenterMaintenance maintenance = maintenanceService.findFirstByMachineAndDate(workCenter, dateTime.toLocalDate());
                timeslot.setMaintenance(maintenance);
                timeslot.setDateTime(dateTime);
                dateTime = dateTime.plusDays(1);
                timeslot.setProcedure(procedure);
            }
            Map<Integer, List<Timeslot>> procedureMap = timeslots.stream().collect(Collectors.groupingBy(timeslot -> timeslot.getProcedure().getProcedureNo()));
            for (List<Timeslot> list : procedureMap.values()) {
                LocalDateTime max = list.stream().map(Timeslot::getDateTime).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(LocalDateTime.now());
                for (Timeslot timeslot : list) {
                    timeslot.getProcedure().setEndTime(max.plusMinutes(timeslot.getDailyHours()));
                }
            }
        }
        return timeslotRepository.saveAll(timeslots);
    }


    public List<Timeslot> findAllByOrderIn(List<Order> orders) {
        return timeslotRepository.findAllByOrderIn(orders);
    }
}

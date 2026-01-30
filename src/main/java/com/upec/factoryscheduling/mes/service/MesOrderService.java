package com.upec.factoryscheduling.mes.service;

import com.google.common.collect.Lists;
import com.upec.factoryscheduling.aps.entity.*;
import com.upec.factoryscheduling.aps.service.*;
import com.upec.factoryscheduling.common.utils.DateUtils;
import com.upec.factoryscheduling.common.utils.NodeLevelManager;
import com.upec.factoryscheduling.mes.entity.MesOrderTask;
import com.upec.factoryscheduling.mes.entity.MesProcedure;
import com.upec.factoryscheduling.mes.repository.MesOrderRepository;
import com.xkzhangsan.time.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MesOrderService {


    private MesOrderTaskService mesJjOrderTaskService;
    private MesProcedureService mesJjProcedureService;
    private OrderService orderService;
    private OrderTaskService orderTaskService;
    private ProcedureService procedureService;
    private WorkCenterService workCenterService;
    private TimeslotService timeslotService;
    private MesOrderRepository mesOrderRepository;

    @Autowired
    private void setMesJjOrderTaskService(MesOrderTaskService mesJjOrderTaskService) {
        this.mesJjOrderTaskService = mesJjOrderTaskService;
    }

    @Autowired
    private void setMesJjProcedureService(MesProcedureService mesJjProcedureService) {
        this.mesJjProcedureService = mesJjProcedureService;
    }

    @Autowired
    public void setOrderTaskService(OrderTaskService orderTaskService) {
        this.orderTaskService = orderTaskService;
    }

    @Autowired
    public void setProcedureService(ProcedureService procedureService) {
        this.procedureService = procedureService;
    }

    @Autowired
    public void setTimeslotService(TimeslotService timeslotService) {
        this.timeslotService = timeslotService;
    }

    @Autowired
    public void setWorkCenterService(WorkCenterService workCenterService) {
        this.workCenterService = workCenterService;
    }

    @Autowired
    public void setMesOrderRepository(MesOrderRepository mesOrderRepository) {
        this.mesOrderRepository = mesOrderRepository;
    }

    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    public List<Timeslot> syncOrderData(List<String> taskNos) {
        List<Order> orders = queryOrderListNotInApsOrder(taskNos);
        orderService.saveAll(orders);
        List<Task> tasks = mesJjOrderTaskService.queryTaskListNotInApsTask(taskNos);
        orderTaskService.saveAll(tasks);
        List<MesProcedure> mesProcedures = mesJjProcedureService.queryMesProcedureNotInAps(taskNos);
        List<Procedure> procedures = convertProcedures(mesProcedures);
        for (Procedure procedure : procedures) {
            createTimeslot(procedure);
        }
        List<Timeslot> timeslots = new ArrayList<>();
        for (Procedure procedure : procedures) {
            timeslots.add(createTimeslot(procedure));
        }
        return timeslotService.saveTimeslot(timeslots);
    }



    private Timeslot createTimeslot(Procedure procedure) {
        Timeslot timeslot = new Timeslot();
        timeslot.setId(procedure.getTask().getTaskNo() + "_" + procedure.getProcedureNo() + "_" + 1);
        timeslot.setProcedure(procedure);
        timeslot.setStartTime(procedure.getStartTime());
        if (procedure.getTask() != null) {
            timeslot.setPriority(timeslot.getProcedure().getTask().getPriority());
        }
        timeslot.setIndex(1);
        timeslot.setTotal(1);
        timeslot.setProcedureIndex(procedure.getIndex());
        timeslot.setParallel(procedure.isParallel());
        timeslot.setDuration(procedure.getMachineMinutes());
        timeslot.setStartTime(procedure.getStartTime());
        timeslot.setEndTime(procedure.getEndTime());
        if (procedure.getStartTime() != null && procedure.getEndTime() != null) {
            timeslot.setManual(true);
        }
        return timeslot;
    }

    private List<Procedure> convertProcedures(List<MesProcedure> mesProcedures) {
        List<WorkCenter> workCenters = workCenterService.getAllMachines();
        Map<String, WorkCenter> workCenterMap = workCenters.stream().collect(Collectors.toMap(WorkCenter::getId, wc -> wc));
        Map<String, Order> orders = orderService.findAllByOrderNoInConvertToMap(mesProcedures.stream().map(MesProcedure::getOrderNo).distinct().collect(Collectors.toList()));
        Map<String,Task> tasks = orderTaskService.findAllTaskConvertToMap(mesProcedures.stream().map(MesProcedure::getTaskNo).distinct().collect(Collectors.toList()));
        List<Procedure> procedures = new ArrayList<>();
        for (MesProcedure mesProcedure : mesProcedures) {
            if (mesProcedure.getProcedureNo().equals("15")) {
                continue;
            }
            Procedure procedure = new Procedure();
            Integer procedureNo = Integer.parseInt(mesProcedure.getProcedureNo());
            procedure.setId(mesProcedure.getSeq());
            procedure.setProcedureName(mesProcedure.getProcedureName());
            procedure.setStatus(mesProcedure.getProcedureStatus());
            procedure.setProcedureNo(procedureNo);
            procedure.setWorkCenter(workCenterMap.get(mesProcedure.getWorkCenterSeq()));
            procedure.setOrder(orders.get(mesProcedure.getOrderNo()));
            procedure.setTask(tasks.get(mesProcedure.getTaskNo()));
            procedure.setProcedureType(mesProcedure.getProcedureType());
            procedure.setCreateDate(DateUtils.parseDateTime(mesProcedure.getCreatedate()));
            if (StringUtils.hasLength(mesProcedure.getNextProcedureNo())) {
                String[] nextProcedureNos = mesProcedure.getNextProcedureNo().split(",");
                List<Integer> numbers = new ArrayList<>();
                for (String nextProcedureNo : nextProcedureNos) {
                    numbers.add(Integer.parseInt(nextProcedureNo));
                }
                procedure.setNextProcedureNo(numbers);
            }
            if (StringUtils.hasLength(mesProcedure.getPlanStartDate())) {
                procedure.setPlanStartDate(DateUtils.parseLocalDate(mesProcedure.getPlanStartDate()));
            }
            if (StringUtils.hasLength(mesProcedure.getPlanEndDate())) {
                procedure.setPlanEndDate(DateUtils.parseLocalDate(mesProcedure.getPlanEndDate()));
            }
            if (StringUtils.hasLength(mesProcedure.getFactStartDate())) {
                procedure.setStartTime(DateUtils.parseDateTime(mesProcedure.getFactStartDate()));
            }
            if (StringUtils.hasLength(mesProcedure.getFactEndDate())) {
                procedure.setEndTime(DateUtils.parseDateTime(mesProcedure.getFactEndDate()));
            }
            if (mesProcedure.getMachineHours() != null) {
                procedure.setMachineMinutes((int) (Double.parseDouble(mesProcedure.getMachineHours()) * 60));
            }
            if (StringUtils.hasLength(mesProcedure.getHumanHours())) {
                procedure.setHumanMinutes((int) (Double.parseDouble(mesProcedure.getHumanHours()) * 60));
            }
            if (StringUtils.hasLength(mesProcedure.getReworkFlag())) {
                procedure.setRework(mesProcedure.getReworkFlag().equals("1"));
            }
            procedures.add(procedure);
        }
        procedures = procedureService.saveProcedures(procedures);
        Map<String, Procedure> map = procedures.stream()
                .collect(Collectors.toMap(p -> p.getTask().getTaskNo() + "_" + p.getProcedureNo(), m1 -> m1, (p1, p2) -> p1));
        for (Procedure procedure : procedures) {
            List<Integer> numbers = procedure.getNextProcedureNo();
            if (CollectionUtil.isEmpty(numbers)) {
                continue;
            }
            for (Integer number : numbers) {
                Procedure nextProcedure = map.get(procedure.getTask().getTaskNo() + "_" + number);
                if (nextProcedure != null) {
                    if (numbers.size() >= 2) {
                        nextProcedure.setParallel(true);
                    }
                    procedure.addNextProcedure(nextProcedure);
                }
            }
        }
        Map<String, List<Procedure>> maps = procedures.stream().collect(Collectors.groupingBy(procedure -> procedure.getTask().getTaskNo()));
        for (List<Procedure> value : maps.values()) {
            Procedure procedure = value.stream().min(Comparator.comparing(Procedure::getProcedureNo)).orElse(null);
            NodeLevelManager.calculateLevels(procedure);

        }
        return procedureService.saveProcedures(procedures);
    }


    public List<Order> queryOrderListNotInApsOrder(List<String> taskNos) {
        return mesOrderRepository.queryOrderListNotInApsOrder(taskNos);
    }

}

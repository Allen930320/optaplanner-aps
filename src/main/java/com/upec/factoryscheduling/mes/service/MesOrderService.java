package com.upec.factoryscheduling.mes.service;

import com.google.common.collect.Lists;
import com.upec.factoryscheduling.aps.entity.*;
import com.upec.factoryscheduling.aps.service.*;
import com.upec.factoryscheduling.common.utils.DateUtils;
import com.upec.factoryscheduling.common.utils.NodeLevelManager;
import com.upec.factoryscheduling.mes.entity.MesJjOrderTask;
import com.upec.factoryscheduling.mes.entity.MesJjProcedure;
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


    private MesJjOrderTaskService mesJjOrderTaskService;
    private MesJjProcedureService mesJjProcedureService;
    private OrderService orderService;
    private OrderTaskService orderTaskService;
    private ProcedureService procedureService;
    private WorkCenterService workCenterService;
    private TimeslotService timeslotService;

    @Autowired
    private void setMesJjOrderTaskService(MesJjOrderTaskService mesJjOrderTaskService) {
        this.mesJjOrderTaskService = mesJjOrderTaskService;
    }

    @Autowired
    private void setMesJjProcedureService(MesJjProcedureService mesJjProcedureService) {
        this.mesJjProcedureService = mesJjProcedureService;
    }

    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
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

    public List<Timeslot> mergePlannerData(List<Order> orders) {
        List<MesJjOrderTask> mesOrderTasks = getOrderTasks(orders);
        List<MesJjProcedure> mesProcedures = getProcedures(mesOrderTasks);
        List<WorkCenter> workCenters = workCenterService.getAllMachines();
        List<Task> tasks = convertTasks(mesOrderTasks);
        Map<String, Order> orderMap = orders.stream().collect(Collectors.toMap(Order::getOrderNo, order -> order));
        Map<String, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getTaskNo, task -> task));
        List<Procedure> procedures = convertProcedures(
                mesProcedures.stream().distinct().collect(Collectors.toList()),
                workCenters,
                orderMap,
                taskMap);
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
        if (procedure.getStartTime() != null && procedure.getEndTime() != null) {
            timeslot.setManual(true);
            timeslot.setStartTime(procedure.getStartTime());
        }
        return timeslot;
    }


    private List<MesJjOrderTask> getOrderTasks(List<Order> orders) {
        List<MesJjOrderTask> orderTasks = new ArrayList<>();
        Lists.partition(orders, 999).forEach(taskNo -> {
            List<String> orderNos = taskNo.stream().filter(Objects::nonNull).map(Order::getOrderNo).collect(Collectors.toList());
            orderTasks.addAll(mesJjOrderTaskService.queryAllByOrderNoInAndTaskStatusIn(orderNos, List.of("生产中", "待生产")));
        });
        return orderTasks;
    }

    private List<MesJjProcedure> getProcedures(List<MesJjOrderTask> orderTasks) {
        List<String> taskNos =
                orderTasks.stream().map(MesJjOrderTask::getTaskNo).distinct().collect(Collectors.toList());
        List<MesJjProcedure> procedures = new ArrayList<>();
        Lists.partition(taskNos, 999).forEach(taskNo -> procedures.addAll(mesJjProcedureService.findAllByTaskNo(taskNo)));
        return procedures;
    }

    private List<Task> convertTasks(List<MesJjOrderTask> mesOrderTasks) {
        List<Task> tasks = new ArrayList<>();
        for (MesJjOrderTask orderTask : mesOrderTasks) {
            Task task = new Task();
            task.setOrderNo(orderTask.getOrderNo());
            task.setTaskNo(orderTask.getTaskNo());
            task.setStatus(orderTask.getTaskStatus());
            task.setCreateDate(DateUtils.parseDateTime(orderTask.getCreateDate()));
            if (StringUtils.hasLength(orderTask.getPlanStartDate())) {
                task.setPlanStartDate(DateUtils.parseLocalDate(orderTask.getPlanStartDate()));
            }
            if (StringUtils.hasLength(orderTask.getPlanEndDate())) {
                task.setPlanEndDate(DateUtils.parseLocalDate(orderTask.getPlanEndDate()));
            }
            if (StringUtils.hasLength(orderTask.getFactStartDate())) {
                task.setFactStartDate(DateUtils.parseDateTime(orderTask.getFactStartDate()));
            }
            if (StringUtils.hasLength(orderTask.getFactEndDate())) {
                task.setFactEndDate(DateUtils.parseDateTime(orderTask.getFactEndDate()));
            }
            if (StringUtils.hasLength(orderTask.getMark())) {
                task.setPriority(100);
            }
            if (StringUtils.hasLength(orderTask.getPlanQuantity())) {
                task.setPlanQuantity(Integer.parseInt(orderTask.getPlanQuantity()));
            }
            if (StringUtils.hasLength(orderTask.getLockedRemark())) {
                task.setLockedRemark(orderTask.getLockedRemark());
            }
            if (StringUtils.hasLength(orderTask.getRouteSeq())) {
                task.setRouteId(orderTask.getRouteSeq());
            }
            tasks.add(task);
        }
        return orderTaskService.saveAll(tasks);
    }

    private List<Procedure> convertProcedures(List<MesJjProcedure> mesProcedures, List<WorkCenter> workCenters,
                                              Map<String, Order> orders, Map<String, Task> tasks) {
        Map<String, WorkCenter> workCenterMap = workCenters.stream().collect(Collectors.toMap(WorkCenter::getId, workCenter -> workCenter));
        List<Procedure> procedures = new ArrayList<>();
        for (MesJjProcedure mesProcedure : mesProcedures) {
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
}

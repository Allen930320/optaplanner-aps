package com.upec.factoryscheduling.mes.service;

import com.google.common.collect.Lists;
import com.upec.factoryscheduling.aps.entity.*;
import com.upec.factoryscheduling.aps.service.*;
import com.upec.factoryscheduling.mes.entity.*;
import com.upec.factoryscheduling.mes.repository.MesOrderRepository;
import com.upec.factoryscheduling.utils.DateUtils;
import com.upec.factoryscheduling.utils.RandomFun;
import com.xkzhangsan.time.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MesOrderService {


    private MesOrderRepository mesOrderRepository;
    private MesBaseWorkCenterService mesBaseWorkCenterService;
    private MesJjOrderTaskService mesJjOrderTaskService;
    private MesJjProcedureService mesJjProcedureService;
    private OrderService orderService;
    private OrderTaskService orderTaskService;
    private ProcedureService procedureService;
    private WorkCenterService workCenterService;
    private TimeslotService timeslotService;
    private ApsWorkCenterMaintenanceService apsWorkCenterMaintenanceService;
    private WorkCenterMaintenanceService workCenterMaintenanceService;
    private MesJjRouteProcedureService mesJjRouteProcedureService;

    @Autowired
    public void setMesOrderRepository(MesOrderRepository mesOrderRepository) {
        this.mesOrderRepository = mesOrderRepository;
    }

    @Autowired
    private void setMesBaseWorkCenterService(MesBaseWorkCenterService mesBaseWorkCenterService) {
        this.mesBaseWorkCenterService = mesBaseWorkCenterService;
    }

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


    @Autowired
    public void setApsWorkCenterMaintenanceService(ApsWorkCenterMaintenanceService apsWorkCenterMaintenanceService) {
        this.apsWorkCenterMaintenanceService = apsWorkCenterMaintenanceService;
    }

    @Autowired
    public void setWorkCenterMaintenanceService(WorkCenterMaintenanceService workCenterMaintenanceService) {
        this.workCenterMaintenanceService = workCenterMaintenanceService;
    }

    @Autowired
    public void setMesJjRouteProcedureService(MesJjRouteProcedureService mesJjRouteProcedureService) {
        this.mesJjRouteProcedureService = mesJjRouteProcedureService;
    }

    public List<Timeslot> mergePlannerData(List<String> taskNos) {

        List<MesJjOrderTask> mesOrderTasks = getOrderTasks(taskNos);
        List<MesJjOrder> mesOrders =
                getMesOrders(mesOrderTasks.stream().map(MesJjOrderTask::getOrderNo).distinct().collect(Collectors.toList()));
        List<MesJjProcedure> mesProcedures = getProcedures(mesOrderTasks);
        List<String> workCenterCodes =
                mesProcedures.stream().map(MesJjProcedure::getWorkCenterSeq).distinct().collect(Collectors.toList());
        List<MesBaseWorkCenter> mesBaseWorkCenters = mesBaseWorkCenterService.findByIdIn(workCenterCodes);
        List<ApsWorkCenterMaintenance> apsWorkCenterMaintenances =
                apsWorkCenterMaintenanceService.findAllByWorkCenterCodeIn(mesBaseWorkCenters.stream()
                        .map(MesBaseWorkCenter::getWorkCenterCode).distinct().collect(Collectors.toList()));
        List<WorkCenter> workCenters = convertWorkCenters(mesBaseWorkCenters);
        convertMaintenance(apsWorkCenterMaintenances, workCenters);
        List<Order> orders = convertOrders(mesOrders);
        List<Task> tasks = convertTasks(mesOrderTasks);
        List<Procedure> procedures =
                convertProcedures(mesProcedures.stream().distinct().collect(Collectors.toList()), workCenters);
        List<Timeslot> timeslots = new ArrayList<>();
        Map<String, Order> orderMap = orders.stream().collect(Collectors.toMap(Order::getOrderNo, order -> order));
        Map<String, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getTaskNo, task -> task));

        // 为每个工序创建分片Timeslot
        for (Procedure procedure : procedures) {
            // 跳过没有绑定工作中心的工序
            if (procedure.getWorkCenterId() == null) {
                log.info("跳过未绑定工作中心的工序: {}", procedure.getId());
                continue;
            }

            // 检查是否有固定的开始时间和结束时间
            if (procedure.getStartTime() != null && procedure.getEndTime() != null) {
                // 情况1：工序有固定的开始时间和结束时间
                // 只创建一个分片
                Timeslot timeslot = createSingleTimeslot(procedure, orderMap, taskMap, 0, 1);

                // 对于machineHours=0的工序，设置duration为0，开始时间和结束时间相同
                if (procedure.getMachineHours() == 0) {
                    timeslot.setDuration(0.0);
                } else {
                    timeslot.setDuration(procedure.getMachineHours()); // 容量即为machineHours
                }

                timeslot.setStartTime(procedure.getStartTime()); // 设置固定的开始时间
                timeslot.setEndTime(procedure.getEndTime()); // 设置固定的结束时间
                timeslot.setManual(true); // 标记为手动设置，不需要参与规划
                timeslots.add(timeslot);
            } else if (procedure.getStartTime() != null) {
                // 情况2：工序只有固定的开始时间

                // 对于machineHours=0的工序，特殊处理
                if (procedure.getMachineHours() == 0) {
                    Timeslot timeslot = createSingleTimeslot(procedure, orderMap, taskMap, 0, 1);
                    timeslot.setDuration(0.0);
                    timeslot.setStartTime(procedure.getStartTime());
                    timeslot.setEndTime(procedure.getStartTime()); // 开始时间和结束时间相同
                    timeslot.setManual(true);
                    timeslots.add(timeslot);
                } else {
                    // 计算工序持续时间（小时）
                    double duration = calculateProcedureDuration(procedure);

                    if (duration <= 1.0) {
                        // 持续时间小于等于1小时的工序不需要分片
                        Timeslot timeslot = createSingleTimeslot(procedure, orderMap, taskMap, 0, 1);
                        timeslot.setStartTime(procedure.getStartTime()); // 设置固定的开始时间
                        timeslot.setManual(true); // 标记为手动设置，开始时间必须以此为准
                        timeslots.add(timeslot);
                    } else {
                        // 持续时间大于1小时的工序需要分片
                        int totalSlices = (int) Math.ceil(duration); // 按小时分片
                        for (int i = 0; i < totalSlices; i++) {
                            Timeslot timeslot = createSingleTimeslot(procedure, orderMap, taskMap, i, totalSlices);
                            // 设置当前分片的持续时间，最后一个分片可能不足1小时
                            timeslot.setDuration(i == totalSlices - 1 ? duration - i : 1.0);
                            // 只有第一个分片需要设置固定的开始时间
                            if (i == 0) {
                                timeslot.setStartTime(procedure.getStartTime()); // 设置固定的开始时间
                                timeslot.setManual(true); // 标记为手动设置
                            }
                            timeslots.add(timeslot);
                        }
                    }
                }
            } else {
                // 情况3：工序没有固定时间，按原逻辑处理

                // 对于machineHours=0的工序，特殊处理
                if (procedure.getMachineHours() == 0) {
                    Timeslot timeslot = createSingleTimeslot(procedure, orderMap, taskMap, 0, 1);
                    timeslot.setDuration(0.0);
                    timeslot.setManual(true);
                    timeslots.add(timeslot);
                } else {
                    // 计算工序持续时间（小时）
                    double duration = calculateProcedureDuration(procedure);

                    if (duration <= 1.0) {
                        // 持续时间小于等于1小时的工序不需要分片
                        Timeslot timeslot = createSingleTimeslot(procedure, orderMap, taskMap, 0, 1);
                        timeslots.add(timeslot);
                    } else {
                        // 持续时间大于1小时的工序需要分片
                        int totalSlices = (int) Math.ceil(duration); // 按小时分片
                        for (int i = 0; i < totalSlices; i++) {
                            Timeslot timeslot = createSingleTimeslot(procedure, orderMap, taskMap, i, totalSlices);
                            // 设置当前分片的持续时间，最后一个分片可能不足1小时
                            timeslot.setDuration(i == totalSlices - 1 ? duration - i : 1.0);
                            timeslots.add(timeslot);
                        }
                    }
                }
            }
        }
        return timeslotService.saveAll(timeslots);
    }

    /**
     * 创建单个时间槽
     */
    private Timeslot createSingleTimeslot(Procedure procedure, Map<String, Order> orderMap, Map<String, Task> taskMap,
            int sliceIndex, int totalSlices) {
        Timeslot timeslot = new Timeslot();
        // 使用工序ID加分片索引作为时间槽ID
        timeslot.setId(procedure.getId() + "_slice_" + sliceIndex);
        timeslot.setProcedure(procedure);
        timeslot.setWorkCenter(procedure.getWorkCenterId());
        timeslot.setOrder(orderMap.get(procedure.getOrderNo()));
        timeslot.setTask(taskMap.get(procedure.getTaskNo()));
        if (timeslot.getTask() != null) {
            timeslot.setPriority(timeslot.getTask().getPriority());
        }
        timeslot.setSliceIndex(sliceIndex);
        timeslot.setTotalSlices(totalSlices);
        timeslot.setDuration(1.0); // 默认持续时间为1小时，最后一个分片会重新设置
        timeslot.setProcedureOrder(procedure.getProcedureNo());
        return timeslot;
    }

    /**
     * 计算工序持续时间（小时）
     */
    private double calculateProcedureDuration(Procedure procedure) {
        // 优先使用machineHours字段的值
        if (procedure.getMachineHours() > 0) {
            return procedure.getMachineHours();
        }

        // 如果没有machineHours，使用默认值1小时
        return 1.0;
    }


    private List<MesJjOrderTask> getOrderTasks(List<String> taskNos) {
        List<MesJjOrderTask> orderTasks = new ArrayList<>();
        Lists.partition(taskNos, 999).forEach(taskNo -> {
            orderTasks.addAll(mesJjOrderTaskService.queryAllByTaskNoInAndTaskStatusIn(taskNo, List.of("生产中")));
        });
        return orderTasks;
    }

    private List<MesJjProcedure> getProcedures(List<MesJjOrderTask> orderTasks) {
        List<String> taskNos =
                orderTasks.stream().map(MesJjOrderTask::getTaskNo).distinct().collect(Collectors.toList());
        List<MesJjProcedure> procedures = new ArrayList<>();
        Lists.partition(taskNos, 999).forEach(taskNo -> {
            procedures.addAll(mesJjProcedureService.findAllByTaskNo(taskNo));
        });
        return procedures;
    }

    private List<MesJjRouteProcedure> getRouteProcedures(List<MesJjProcedure> procedures) {
        List<String> routeSeqs =
                procedures.stream().map(MesJjProcedure::getRouteSeq).distinct().collect(Collectors.toList());
        List<MesJjRouteProcedure> routeProcedures = new ArrayList<>();
        Lists.partition(routeSeqs, 999).forEach(routeSeq -> {
            routeProcedures.addAll(mesJjRouteProcedureService.findAllByRouteSeqIn(routeSeq));
        });
        return routeProcedures;
    }


    private List<WorkCenterMaintenance> convertMaintenance(List<ApsWorkCenterMaintenance> apsWorkCenterMaintenances,
            List<WorkCenter> workCenters) {
        Map<String, WorkCenter> workCenterMap =
                workCenters.stream().collect(Collectors.toMap(WorkCenter::getWorkCenterCode, workCenter -> workCenter));
        List<WorkCenterMaintenance> maintenances = new ArrayList<>();
        for (ApsWorkCenterMaintenance apsWorkCenterMaintenance : apsWorkCenterMaintenances) {
            WorkCenterMaintenance maintenance = new WorkCenterMaintenance();
            maintenance.setYear(DateUtils.parseLocalDate(apsWorkCenterMaintenance.getLocalDate()).getYear());
            maintenance.setCapacity(apsWorkCenterMaintenance.getCapacity().intValue());
            maintenance.setDate(DateUtils.parseLocalDate(apsWorkCenterMaintenance.getLocalDate()));
            maintenance.setWorkCenter(workCenterMap.get(apsWorkCenterMaintenance.getWorkCenterCode()));
            maintenance.setStartTime(DateUtils.parseDateTime(apsWorkCenterMaintenance.getStartTime()).toLocalTime());
            maintenance.setEndTime(DateUtils.parseDateTime(apsWorkCenterMaintenance.getEndTime()).toLocalTime());
            maintenance.setStatus(apsWorkCenterMaintenance.getStatus());
            maintenance.setId(RandomFun.getInstance().getRandom());
            maintenances.add(maintenance);
        }
        return workCenterMaintenanceService.createMachineMaintenance(maintenances);
    }

    private List<Order> convertOrders(List<MesJjOrder> mesOrders) {
        List<Order> orders = new ArrayList<>();
        for (MesJjOrder mesOrder : mesOrders) {
            Order order = new Order();
            order.setOrderNo(mesOrder.getOrderNo());
            order.setOrderStatus(mesOrder.getOrderStatus());
            order.setErpStatus(mesOrder.getErpStatus());
            if (StringUtils.hasLength(mesOrder.getPlanStartDate())) {
                order.setPlanStartDate(DateUtils.parseLocalDate(mesOrder.getPlanStartDate()));
            }
            if (StringUtils.hasLength(mesOrder.getPlanEndDate())) {
                order.setPlanEndDate(DateUtils.parseLocalDate(mesOrder.getPlanEndDate()));
            }
            if (StringUtils.hasLength(mesOrder.getFactStartDate())) {
                order.setFactStartDate(DateUtils.parseDateTime(mesOrder.getFactStartDate()));
            }
            if (StringUtils.hasLength(mesOrder.getFactEndDate())) {
                order.setFactEndDate(DateUtils.parseDateTime(mesOrder.getFactEndDate()));
            }
            orders.add(order);
        }
        return orderService.saveAll(orders);
    }

    private List<Task> convertTasks(List<MesJjOrderTask> mesOrderTasks) {
        List<Task> tasks = new ArrayList<>();
        for (MesJjOrderTask orderTask : mesOrderTasks) {
            Task task = new Task();
            task.setOrderNo(orderTask.getOrderNo());
            task.setTaskNo(orderTask.getTaskNo());
            task.setStatus(orderTask.getTaskStatus());
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
            tasks.add(task);
        }
        return orderTaskService.saveAll(tasks);
    }

    private List<Procedure> convertProcedures(List<MesJjProcedure> mesProcedures, List<WorkCenter> workCenters) {
        Map<String, WorkCenter> workCenterMap =
                workCenters.stream().collect(Collectors.toMap(WorkCenter::getId, workCenter -> workCenter));
        List<MesJjRouteProcedure> routeProcedures = getRouteProcedures(mesProcedures);
        Map<String, MesJjRouteProcedure> routeProcedureMap = routeProcedures.stream()
                .collect(Collectors.toMap(m -> m.getRouteSeq() + "_" + m.getProcedureNo(), m -> m));
        List<Procedure> procedures = new ArrayList<>();
        for (MesJjProcedure mesProcedure : mesProcedures) {
            MesJjRouteProcedure routeProcedure =
                    routeProcedureMap.get(mesProcedure.getRouteSeq() + "_" + mesProcedure.getProcedureNo());
            Procedure procedure = new Procedure();
            Integer procedureNo = Integer.parseInt(mesProcedure.getProcedureNo());
            procedure.setId(mesProcedure.getSeq());
            procedure.setOrderNo(mesProcedure.getOrderNo());
            procedure.setTaskNo(mesProcedure.getTaskNo());
            procedure.setWorkCenterId(workCenterMap.get(mesProcedure.getWorkCenterSeq()));
            procedure.setProcedureName(mesProcedure.getProcedureName());
            procedure.setStatus(mesProcedure.getProcedureStatus());
            procedure.setProcedureNo(procedureNo);
            if (StringUtils.hasLength(mesProcedure.getNextProcedureNo())) {
                String[] nextProcedureNos = mesProcedure.getNextProcedureNo().split(",");
                List<Integer> numbers = new ArrayList<>();
                for (String nextProcedureNo : nextProcedureNos) {
                    numbers.add(Integer.parseInt(nextProcedureNo));
                }
                procedure.setNextProcedureNo(numbers);
            }
            if (CollectionUtil.isNotEmpty(procedure.getNextProcedureNo())
                    && procedure.getNextProcedureNo().size() > 1) {
                procedure.setParallel(true);
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
            if (routeProcedure != null && routeProcedure.getMachineHours() != null) {
                procedure.setMachineHours(Double.parseDouble(routeProcedure.getMachineHours()));
            }
            procedures.add(procedure);
        }
        procedures = procedureService.saveProcedures(procedures);
        Map<String, Procedure> map = procedures.stream()
                .collect(Collectors.toMap(p -> p.getTaskNo() + "_" + p.getProcedureNo(), m1 -> m1, (p1, p2) -> p1));
        for (Procedure procedure : procedures) {
            List<Integer> numbers = procedure.getNextProcedureNo();
            if (CollectionUtil.isEmpty(numbers)) {
                continue;
            }
            List<Procedure> nextProcedures = new ArrayList<>();
            for (Integer number : numbers) {
                Procedure nextProcedure = map.get(procedure.getTaskNo() + "_" + number);
                if (nextProcedure != null) {
                    nextProcedures.add(nextProcedure);
                }
            }
            procedure.setNextProcedure(nextProcedures);
        }
        return procedureService.saveProcedures(procedures);
    }

    private List<WorkCenter> convertWorkCenters(List<MesBaseWorkCenter> mesBaseWorkCenters) {
        List<WorkCenter> workCenters = new ArrayList<>();
        for (MesBaseWorkCenter baseWorkCenter : mesBaseWorkCenters) {
            WorkCenter workCenter = new WorkCenter();
            workCenter.setId(baseWorkCenter.getSeq());
            workCenter.setName(baseWorkCenter.getDescription());
            workCenter.setWorkCenterCode(baseWorkCenter.getWorkCenterCode());
            workCenter.setStatus(baseWorkCenter.getStatus());
            workCenters.add(workCenter);
        }
        return workCenterService.saveWorkCenters(workCenters);
    }

    public List<MesJjOrder> getMesOrders(List<String> orderNos) {
        if (CollectionUtil.isNotEmpty(orderNos)) {
            return mesOrderRepository.findAllByOrderNoIn(orderNos);
        }
        return mesOrderRepository.findAllByPlanStartDateAfterAndOrderStatusIn("2025-01-01", List.of("生产中"));
    }
}

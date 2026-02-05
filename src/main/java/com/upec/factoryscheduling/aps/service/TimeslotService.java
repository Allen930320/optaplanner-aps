package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.dto.TaskTimeslotDTO;
import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.repository.TimeslotRepository;
import com.upec.factoryscheduling.aps.resquest.ProcedureRequest;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import com.upec.factoryscheduling.auth.entity.User;
import com.upec.factoryscheduling.common.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TimeslotService {

    private TimeslotRepository timeslotRepository;

    @Autowired
    private void setTimeslotRepository(TimeslotRepository timeslotRepository) {
        this.timeslotRepository = timeslotRepository;
    }


    private OrderTaskService orderTaskService;

    @Autowired
    public void setOrderTaskService(OrderTaskService orderTaskService) {
        this.orderTaskService = orderTaskService;
    }

    private ProcedureService procedureService;

    @Autowired
    public void setProcedureService(ProcedureService procedureService) {
        this.procedureService = procedureService;
    }

    @Transactional
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


    @Transactional
    public List<Timeslot> saveAll(List<Timeslot> timeslots) {
        return timeslotRepository.saveAll(timeslots);
    }

    public List<Timeslot> findListByProcedure(Procedure procedure) {
        return timeslotRepository.findAllByProcedure(procedure);
    }

    public void save(Timeslot timeslot) {
        timeslotRepository.save(timeslot);
    }

    @Transactional
    public void deleteAll() {
        timeslotRepository.deleteAll();
    }

    @Transactional
    public List<Timeslot> saveTimeslot(List<Timeslot> timeslots) {
        return timeslotRepository.saveAll(timeslots);
    }

    public List<Timeslot> findAllByTaskIn(List<String> taskNos) {
        Sort sort = Sort.by(Sort.Direction.DESC, "procedureIndex", "index").ascending();
        List<String> procedureTypes = List.of("ZP01", "ZP02");
        return timeslotRepository.findAllByProcedure_Task_TaskNoIsInAndProcedure_ProcedureTypeIsIn(taskNos,
                procedureTypes, sort);
    }

    @Transactional
    public String createTimeslot(String procedureId, double time, int slice) {
        List<Timeslot> timeslots = timeslotRepository.findAllByProcedure_Id(procedureId);
        if (CollectionUtils.isEmpty(timeslots)) {
            return "未查询到相关工序!";
        }
        Procedure procedure = procedureService.findProcedureById(procedureId);
        if (procedure == null) {
            return "未查询到相关工序!";
        }
        if ("ZP02".equals(procedure.getProcedureType())) {
            arrangeOutsourcing(timeslots, slice);
            return null;
        } else {
            splitTimeslot(timeslots, slice, procedure.getMachineMinutes());
        }
        return null;
    }


    private void arrangeOutsourcing(List<Timeslot> timeslots, int slice) {
        splitTimeslot(timeslots, slice);
        for (Timeslot ts : timeslots) {
            ts.setTotal(timeslots.size());
        }
        timeslotRepository.saveAll(timeslots);
    }

    private List<Timeslot> splitTimeslot(Timeslot timeslot, List<Timeslot> others, double time) {
        time = time * 60;
        List<Timeslot> timeslots = new ArrayList<>();
        int duration = timeslot.getDuration();
        int index = timeslot.getTotal();
        if (time >= duration) {
            timeslots.add(timeslot);
            return timeslots;
        }
        duration = duration - (int) time;
        timeslot.setDuration((int) time);
        timeslots.add(timeslot);
        timeslots.addAll(others);
        while (duration > 0) {
            Timeslot newTimeslot = new Timeslot();
            BeanUtils.copyProperties(timeslot, newTimeslot);
            index++;
            newTimeslot.setId(timeslot.getProcedure().getTask().getTaskNo() + "_" + timeslot.getProcedure().getProcedureNo() + "_" + index);
            newTimeslot.setDuration(Math.min(duration, (int) time));
            newTimeslot.setIndex(index);
            timeslots.add(newTimeslot);
            duration = duration - (int) time;
        }
        int total = timeslots.size();
        return timeslots.stream().peek(t -> t.setTotal(total)).collect(Collectors.toList());
    }

    private void splitTimeslot(List<Timeslot> timeslots, int slice, int duration) {
        splitTimeslot(timeslots, slice);
        int[] interval = splitNumber(duration, slice);
        for (int i = 0; i < timeslots.size(); i++) {
            Timeslot ts = timeslots.get(i);
            ts.setTotal(timeslots.size());
            ts.setDuration(interval[i]);
            timeslotRepository.save(ts);
        }
    }

    private void splitTimeslot(List<Timeslot> timeslots, int slice) {
        int size = timeslots.size();
        if (slice == size) {
            return;
        }
        if (slice > size) {
            Timeslot timeslot = timeslots.get(size - 1);
            for (; size < slice; size++) {
                Timeslot ts = new Timeslot();
                BeanUtils.copyProperties(timeslot, ts);
                ts.setId(timeslot.getProcedure().getTask().getTaskNo() + "_" + timeslot.getProcedure().getProcedureNo() +
                        "_" + (size + 1));
                ts.setIndex(size + 1);
                timeslots.add(ts);
            }
        }
        if (slice < size) {
            Map<Integer, Timeslot> map = timeslots.stream().collect(Collectors.toMap(Timeslot::getIndex, timeslot -> timeslot));
            for (; slice < size; slice++) {
                Timeslot ts = map.get(slice + 1);
                timeslots.remove(ts);
                timeslotRepository.delete(ts);
            }
        }
    }

    private int[] splitNumber(int total, int parts) {
        int[] result = new int[parts];
        int base = total / parts;
        int remainder = total % parts;
        for (int i = 0; i < parts; i++) {
            result[i] = base + (i < remainder ? 1 : 0);
        }
        return result;
    }

    public void splitOutsourcingTimeslot(String timeId, int days) {
        Timeslot timeslot = timeslotRepository.findById(timeId).orElse(null);
        if (timeslot == null) {
            return;
        }
        timeslot.setDuration(480);
        timeslotRepository.save(timeslot);
        Procedure procedure = timeslot.getProcedure();
        List<Timeslot> timeslots = timeslotRepository.findAllByProcedure(procedure);
        timeslot = timeslots.stream().max(Comparator.comparing(Timeslot::getIndex)).orElse(timeslot);
        timeslot.setDuration(480);
        timeslotRepository.save(timeslot);
        int index = timeslot.getIndex();
        for (int i = 1; i < days; i++) {
            Timeslot newTimeslot = new Timeslot();
            BeanUtils.copyProperties(timeslot, newTimeslot);
            index++;
            newTimeslot.setId(timeslot.getProcedure().getTask().getTaskNo() + "_" + timeslot.getProcedure().getProcedureNo() + "_" + index);
            newTimeslot.setIndex(index);
            timeslotRepository.save(newTimeslot);
        }
    }


    public Page<TaskTimeslotDTO> queryTimeslots(String productName,
                                                String productCode,
                                                String taskNo,
                                                String contractNum,
                                                String startTime,
                                                String endTime,
                                                Integer pageNum,
                                                Integer pageSize) {
        Page<TaskTimeslotDTO> page = orderTaskService.queryTaskWithTimeslot(productName, productCode, taskNo, contractNum, startTime, endTime, pageNum, pageSize);
        List<TaskTimeslotDTO> dtos = page.getContent();
        if (!CollectionUtils.isEmpty(dtos)) {
            List<String> taskNos = dtos.stream().map(TaskTimeslotDTO::getTaskNo).collect(Collectors.toList());
            List<Timeslot> timeslots = timeslotRepository.findAllByProcedure_Task_TaskNoIsIn(taskNos);
            Map<String, List<Timeslot>> map = timeslots.stream().collect(Collectors.groupingBy(timeslot -> timeslot.getProcedure().getTask().getTaskNo()));
            page.get().peek(m -> m.setTimeslots(map.get(m.getTaskNo()))).collect(Collectors.toList());
        }
        return page;
    }


    public Page<TaskTimeslotDTO> queryTimeslotsByProductUser(String productName,
                                                             String productCode,
                                                             String taskNo,
                                                             String contractNum,
                                                             String startTime,
                                                             String endTime,
                                                             Integer pageNum,
                                                             Integer pageSize) {

        Page<TaskTimeslotDTO> page = orderTaskService.queryTaskWithTimeslotByUser(productName, productCode, taskNo, contractNum, startTime,
                endTime, pageNum, pageSize);
        List<TaskTimeslotDTO> dtos = page.getContent();
        if (!CollectionUtils.isEmpty(dtos)) {
            List<String> taskNos = dtos.stream().map(TaskTimeslotDTO::getTaskNo).collect(Collectors.toList());
            String userName = UserContext.getCurrentUsername();
            List<Timeslot> timeslots = timeslotRepository.queryTimeslots(userName, taskNos);
            Map<String, List<Timeslot>> map = timeslots.stream().collect(Collectors.groupingBy(timeslot -> timeslot.getProcedure().getTask().getTaskNo()));
            page.get().peek(m -> m.setTimeslots(map.get(m.getTaskNo()))).collect(Collectors.toList());
        }
        return page;
    }


}

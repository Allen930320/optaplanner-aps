package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.resquest.ProcedureRequest;
import com.upec.factoryscheduling.aps.service.SchedulingService;
import com.upec.factoryscheduling.aps.service.TimeslotService;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import com.upec.factoryscheduling.common.utils.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.solver.SolverStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/scheduling")
@Slf4j
@CrossOrigin
public class SchedulingController {

    private final SchedulingService schedulingService;


    @Autowired
    public SchedulingController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    private TimeslotService timeslotService;


    @Autowired
    public void setTimeslotService(TimeslotService timeslotService) {
        this.timeslotService = timeslotService;
    }


    @PostMapping("/solve/{problemId}")
    public ApiResponse<String> startScheduling(@PathVariable Long problemId, @RequestBody List<String> orderNos) {
        schedulingService.startScheduling(problemId, orderNos);
        return ApiResponse.success("Scheduling started for problem " + problemId);
    }


    @PostMapping("/stop/{problemId}")
    public ApiResponse<String> stopScheduling(@PathVariable Long problemId) {
        schedulingService.stopScheduling(problemId);
        return ApiResponse.success("Scheduling stopped for problem " + problemId);
    }


    @GetMapping("/solution/{problemId}")
    public ApiResponse<FactorySchedulingSolution> getBestSolution(@PathVariable Long problemId) {
        FactorySchedulingSolution solution = schedulingService.getBestSolution(problemId);
        // 注意：此处有一段无实际作用的循环代码，在实际优化中应移除
        for (Timeslot timeslot : solution.getTimeslots()) {
            int i = 1;
        }
        return ApiResponse.success(solution);
    }


    @GetMapping("/score/{problemId}")
    public ApiResponse<HardMediumSoftScore> getScore(@PathVariable Long problemId) {
        HardMediumSoftScore hardSoftScore = schedulingService.getScore(problemId);
        return ApiResponse.success(hardSoftScore);
    }


    @GetMapping("/status/{problemId}")
    public ApiResponse<String> getStatus(@PathVariable Long problemId) {
        SolverStatus isSolving = schedulingService.isSolving(problemId);
        return ApiResponse.success(isSolving.name());
    }


    @GetMapping("/feasible/{problemId}")
    public ApiResponse<Boolean> isSolutionFeasible(@PathVariable Long problemId) {
        boolean isFeasible = schedulingService.isSolutionFeasible(problemId);
        return ApiResponse.success(isFeasible);
    }

    @PutMapping("/update/{problemId}")
    public ApiResponse<String> updateProblem(@PathVariable Long problemId, @RequestBody FactorySchedulingSolution updatedSolution) {
        schedulingService.updateProblem(problemId, updatedSolution);
        return ApiResponse.success("Problem updated for " + problemId);
    }

    @GetMapping("/explain/{problemId}")
    public ApiResponse<ScoreExplanation<FactorySchedulingSolution, HardMediumSoftScore>> getExplanation(@PathVariable Long problemId) {
        return ApiResponse.success(schedulingService.explainSolution(problemId));
    }


    @PostMapping("/update")
    public ApiResponse<Timeslot> update(@RequestBody ProcedureRequest request) {
        return ApiResponse.success(timeslotService.updateTimeslot(request));
    }

}

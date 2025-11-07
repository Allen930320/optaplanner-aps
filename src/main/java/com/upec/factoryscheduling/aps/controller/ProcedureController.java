package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.service.ProcedureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/procedure")
@CrossOrigin
public class ProcedureController {

    private ProcedureService procedureService;


    @Autowired
    public void setProcessService(ProcedureService processService) {
        this.procedureService = processService;
    }

    @PostMapping
    public ResponseEntity<List<Timeslot>> createProcesses(@RequestBody List<Procedure> procedures) {
        return ResponseEntity.ok(new ArrayList<>());
    }


    @PostMapping("/list")
    public ResponseEntity<List<Timeslot>> createProcedure(@RequestBody List<Procedure> procedures) {
        return ResponseEntity.ok(new ArrayList<>());
    }
}

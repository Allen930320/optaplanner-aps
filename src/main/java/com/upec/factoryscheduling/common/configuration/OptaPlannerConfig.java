package com.upec.factoryscheduling.common.configuration;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import com.upec.factoryscheduling.aps.solver.FactorySchedulingConstraintProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.SolutionManager;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicType;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
import org.optaplanner.core.config.localsearch.LocalSearchType;
import org.optaplanner.core.config.phase.PhaseConfig;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.SolverManagerConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.optaplanner.core.config.solver.SolverConfig.MOVE_THREAD_COUNT_AUTO;
import static org.optaplanner.core.config.solver.SolverConfig.MOVE_THREAD_COUNT_NONE;

@Configuration
public class OptaPlannerConfig {

    @Bean
    public SolverConfig solverConfig() {
        SolverConfig solverConfig = new SolverConfig();

        // 设置解决方案和实体类
        solverConfig.withSolutionClass(FactorySchedulingSolution.class)
                .withEntityClasses(Timeslot.class)
                .withConstraintProviderClass(FactorySchedulingConstraintProvider.class);

        // 设置终止条件
        solverConfig.withTerminationConfig(new TerminationConfig()
                .withSecondsSpentLimit(300L)
                .withBestScoreLimit("0hard/0soft"));
//
//        // 设置阶段配置
//        List<PhaseConfig> phaseConfigList = List.of(
//                // 构造启发式阶段
//                new ConstructionHeuristicPhaseConfig()
//                        .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT),
//
//                // 局部搜索阶段
//                new LocalSearchPhaseConfig()
//                        .withLocalSearchType(LocalSearchType.LATE_ACCEPTANCE)
//                        .withTerminationConfig(new TerminationConfig()
//                                .withUnimprovedSecondsSpentLimit(30L))
//        );

//        solverConfig.setPhaseConfigList(phaseConfigList);

        // 设置环境模式
//        solverConfig.setEnvironmentMode(EnvironmentMode.FAST_ASSERT);

        // 并行求解 - 使用固定线程数而不是AUTO以提高稳定性
        // 在多线程环境中，使用固定线程数可以避免线程创建和销毁的开销
        // 同时确保线程安全，避免moveThreadIndex异常
        solverConfig.setMoveThreadCount("4");

        return solverConfig;
    }

    @Bean
    public SolverManager<FactorySchedulingSolution, Long> solverManager(SolverConfig solverConfig) {
        return SolverManager.create(solverConfig, new SolverManagerConfig());
    }

    @Bean
    public SolutionManager<FactorySchedulingSolution, HardSoftScore> solutionManager(
            SolverManager<FactorySchedulingSolution, Long> solverManager) {
        return SolutionManager.create(solverManager);
    }
}

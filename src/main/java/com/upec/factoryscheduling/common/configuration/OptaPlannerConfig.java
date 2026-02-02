package com.upec.factoryscheduling.common.configuration;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import com.upec.factoryscheduling.aps.solver.FactorySchedulingConstraintProvider;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                .withSecondsSpentLimit(300L)  // 5分钟
                .withBestScoreLimit("0hard/0medium/10000soft")
                .withUnimprovedSecondsSpentLimit(60L));

        // 设置环境模式
        solverConfig.setEnvironmentMode(ai.timefold.solver.core.config.solver.EnvironmentMode.FAST_ASSERT);
        
        // 多线程配置
//        solverConfig.setMoveThreadCount("AUTO");
        
        // 设置随机数种子以提高多线程环境下的稳定性
        solverConfig.setRandomSeed(42L);

        return solverConfig;
    }

    @Bean
    public SolverManager<FactorySchedulingSolution, Long> solverManager(SolverConfig solverConfig) {
        // 创建SolverManager
        return SolverManager.create(solverConfig);
    }

    @Bean
    public SolutionManager<FactorySchedulingSolution, HardMediumSoftScore> solutionManager(SolverManager<FactorySchedulingSolution,
            Long> solverManager) {
        // 使用SolverManager创建SolutionManager
        return SolutionManager.create(solverManager);
    }
}
